package com.medvision.image;

import com.medvision.auth.UserRepository;
import com.medvision.common.error.ApiException;
import com.medvision.storage.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class ImageFileService {

    private final ImageUploadValidator validator;
    private final StorageService storage;
    private final MedicalImageRepository images;
    private final UserRepository users;
    private final TransactionTemplate transactions;

    public ImageFileService(
            ImageUploadValidator validator,
            StorageService storage,
            MedicalImageRepository images,
            UserRepository users,
            PlatformTransactionManager transactionManager
    ) {
        this.validator = validator;
        this.storage = storage;
        this.images = images;
        this.users = users;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    public MedicalImageResponse upload(UUID ownerId, MultipartFile file) {
        var image = validator.validate(file);
        String key = storage.store(image.bytes(), image.extension());

        try {
            // execute returns only after the database transaction commits.
            return transactions.execute(status -> {
                var owner = users.findById(ownerId).orElseThrow(
                        () -> new ApiException(
                                HttpStatus.UNAUTHORIZED,
                                "UNAUTHORIZED",
                                "Tizimga qayta kirish talab qilinadi."
                        )
                );

                var entity = new MedicalImage(
                        owner,
                        image.originalName(),
                        key,
                        image.mimeType(),
                        image.bytes().length,
                        image.width(),
                        image.height()
                );

                return MedicalImageResponse.from(images.saveAndFlush(entity));
            });
        } catch (RuntimeException ex) {
            try {
                storage.delete(key);
            } catch (RuntimeException cleanup) {
                ex.addSuppressed(cleanup);
            }
            throw ex;
        }
    }

    public ImageContent content(UUID ownerId, UUID imageId) {
        var metadata = transactions.execute(status -> {
            var image = images.findByIdAndUser_Id(imageId, ownerId)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "NOT_FOUND",
                            "So'ralgan resurs topilmadi."
                    ));

            return new ContentMetadata(
                    image.getStorageKey(),
                    image.getMimeType(),
                    image.getSizeBytes()
            );
        });

        if (metadata == null) {
            throw new IllegalStateException("Image metadata missing.");
        }

        byte[] bytes = storage.read(metadata.key());
        if (bytes.length != metadata.size()) {
            throw new IllegalStateException("Stored image size mismatch.");
        }

        return new ImageContent(bytes, metadata.mimeType());
    }

    private record ContentMetadata(String key, String mimeType, long size) {
    }

    public record ImageContent(byte[] bytes, String mimeType) {
    }
}
