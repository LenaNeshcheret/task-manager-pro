CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_projects_owner_id
        FOREIGN KEY (owner_id)
        REFERENCES users (id)
        ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_projects_owner_id_name ON projects (owner_id, name);
CREATE INDEX idx_projects_owner_id ON projects (owner_id);
CREATE INDEX idx_projects_created_at ON projects (created_at);

CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    due_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    priority VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_tasks_project_id
        FOREIGN KEY (project_id)
        REFERENCES projects (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_tasks_project_id ON tasks (project_id);
CREATE INDEX idx_tasks_status ON tasks (status);
CREATE INDEX idx_tasks_due_at ON tasks (due_at);
CREATE INDEX idx_tasks_priority ON tasks (priority);

CREATE TABLE reminders (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    scheduled_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(50) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_reminders_task_id
        FOREIGN KEY (task_id)
        REFERENCES tasks (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_reminders_task_id ON reminders (task_id);
CREATE INDEX idx_reminders_status_scheduled_at ON reminders (status, scheduled_at);

CREATE TABLE export_jobs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finished_at TIMESTAMPTZ,
    file_path VARCHAR(1024),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_export_jobs_user_id
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_export_jobs_user_id_created_at ON export_jobs (user_id, created_at);
CREATE INDEX idx_export_jobs_status ON export_jobs (status);
