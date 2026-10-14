ALTER TABLE analyses
    ADD COLUMN dice_score DOUBLE PRECISION,
    ADD COLUMN iou_score DOUBLE PRECISION,
    ADD COLUMN ground_truth_key VARCHAR(500);

ALTER TABLE analyses
    ADD CONSTRAINT chk_analyses_dice_score
    CHECK (
        dice_score IS NULL
        OR (dice_score >= 0 AND dice_score <= 1)
    );

ALTER TABLE analyses
    ADD CONSTRAINT chk_analyses_iou_score
    CHECK (
        iou_score IS NULL
        OR (iou_score >= 0 AND iou_score <= 1)
    );

ALTER TABLE analyses
    ADD CONSTRAINT chk_analyses_evaluation_consistency
    CHECK (
        (
            ground_truth_key IS NULL
            AND dice_score IS NULL
            AND iou_score IS NULL
        )
        OR (
            ground_truth_key IS NOT NULL
            AND dice_score IS NOT NULL
            AND iou_score IS NOT NULL
        )
    );