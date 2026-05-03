CREATE TABLE IF NOT EXISTS course_requests (
    id BIGSERIAL PRIMARY KEY,
    faculty_id BIGINT NOT NULL,
    requested_course_title VARCHAR(255) NOT NULL,
    requested_course_code VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    rejection_reason TEXT,
    processed_by_user_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_course_requests_faculty FOREIGN KEY (faculty_id) REFERENCES faculties(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_course_requests_faculty_id ON course_requests (faculty_id);
CREATE INDEX IF NOT EXISTS idx_course_requests_status ON course_requests (status);
CREATE INDEX IF NOT EXISTS idx_course_requests_created_at ON course_requests (created_at);

