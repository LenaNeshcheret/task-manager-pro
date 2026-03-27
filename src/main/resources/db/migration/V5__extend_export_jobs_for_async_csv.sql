ALTER TABLE export_jobs
    ADD COLUMN project_id BIGINT,
    ADD COLUMN error_message TEXT;

ALTER TABLE export_jobs
    ADD CONSTRAINT fk_export_jobs_project_id
        FOREIGN KEY (project_id)
        REFERENCES projects (id)
        ON DELETE CASCADE;

UPDATE export_jobs
SET status = 'DONE'
WHERE status = 'COMPLETED';

CREATE INDEX IF NOT EXISTS idx_export_jobs_user_id_status ON export_jobs (user_id, status);
CREATE INDEX IF NOT EXISTS idx_export_jobs_project_id_created_at ON export_jobs (project_id, created_at);
CREATE INDEX IF NOT EXISTS idx_export_jobs_type_status ON export_jobs (type, status);
