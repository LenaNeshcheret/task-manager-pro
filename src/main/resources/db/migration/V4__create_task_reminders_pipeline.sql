ALTER TABLE reminders RENAME TO task_reminders;

ALTER TABLE task_reminders
    ADD COLUMN last_error TEXT,
    ADD COLUMN sent_at TIMESTAMPTZ;

ALTER TABLE task_reminders
    ADD CONSTRAINT uq_task_reminders_task_id_scheduled_at UNIQUE (task_id, scheduled_at);

ALTER INDEX idx_reminders_task_id RENAME TO idx_task_reminders_task_id;
ALTER INDEX idx_reminders_status_scheduled_at RENAME TO idx_task_reminders_status_scheduled_at;
