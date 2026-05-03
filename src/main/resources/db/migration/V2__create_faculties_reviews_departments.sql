-- Departments
CREATE TABLE IF NOT EXISTS departments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    CONSTRAINT uk_departments_name UNIQUE (name)
);

-- Faculties (profiles)
CREATE TABLE IF NOT EXISTS faculties (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NULL,
    name VARCHAR(255) NOT NULL,
    department_id BIGINT NULL,
    designation VARCHAR(255),
    bio TEXT,
    image_path VARCHAR(1024),
    CONSTRAINT fk_faculties_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_faculties_name ON faculties (name);
CREATE INDEX IF NOT EXISTS idx_faculties_department ON faculties (department_id);

-- Reviews
CREATE TABLE IF NOT EXISTS reviews (
    id BIGSERIAL PRIMARY KEY,
    reviewer_user_id BIGINT NOT NULL,
    faculty_id BIGINT NOT NULL,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_reviews_reviewer FOREIGN KEY (reviewer_user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_faculty FOREIGN KEY (faculty_id) REFERENCES faculties(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_reviews_faculty_id ON reviews (faculty_id);
CREATE INDEX IF NOT EXISTS idx_reviews_reviewer_id ON reviews (reviewer_user_id);

-- Seed some departments
INSERT INTO departments (name) VALUES ('CS') ON CONFLICT (name) DO NOTHING;
INSERT INTO departments (name) VALUES ('AI & DS') ON CONFLICT (name) DO NOTHING;
INSERT INTO departments (name) VALUES ('SE') ON CONFLICT (name) DO NOTHING;
INSERT INTO departments (name) VALUES ('Cyber Security') ON CONFLICT (name) DO NOTHING;
INSERT INTO departments (name) VALUES ('Science And Humanities') ON CONFLICT (name) DO NOTHING;

-- Note: initial faculties and reviews can be inserted later via admin UI or another migration

