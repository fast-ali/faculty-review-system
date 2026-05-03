-- Courses master table
CREATE TABLE IF NOT EXISTS courses (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    CONSTRAINT uk_courses_code UNIQUE (code)
);

-- Faculty-course mapping for per-instructor course selection
CREATE TABLE IF NOT EXISTS faculty_courses (
    faculty_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    PRIMARY KEY (faculty_id, course_id),
    CONSTRAINT fk_faculty_courses_faculty FOREIGN KEY (faculty_id) REFERENCES faculties(id) ON DELETE CASCADE,
    CONSTRAINT fk_faculty_courses_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_faculty_courses_faculty ON faculty_courses (faculty_id);
CREATE INDEX IF NOT EXISTS idx_faculty_courses_course ON faculty_courses (course_id);

-- Attach a course and detailed rubric scores to each review
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS course_id BIGINT NULL;
ALTER TABLE reviews ADD CONSTRAINT fk_reviews_course
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE SET NULL;

ALTER TABLE reviews ADD COLUMN IF NOT EXISTS subject_matter_knowledge SMALLINT;
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS teaching_methods SMALLINT;
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS student_engagement SMALLINT;
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS collaboration_teamwork SMALLINT;
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS behavior_management SMALLINT;
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS classroom_environment SMALLINT;
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS professional_ethics SMALLINT;
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS communication_skills SMALLINT;

-- Backfill rubric scores from legacy single rating so historical data remains usable
UPDATE reviews
SET
    subject_matter_knowledge = COALESCE(subject_matter_knowledge, rating),
    teaching_methods = COALESCE(teaching_methods, rating),
    student_engagement = COALESCE(student_engagement, rating),
    collaboration_teamwork = COALESCE(collaboration_teamwork, rating),
    behavior_management = COALESCE(behavior_management, rating),
    classroom_environment = COALESCE(classroom_environment, rating),
    professional_ethics = COALESCE(professional_ethics, rating),
    communication_skills = COALESCE(communication_skills, rating);

ALTER TABLE reviews ALTER COLUMN subject_matter_knowledge SET NOT NULL;
ALTER TABLE reviews ALTER COLUMN teaching_methods SET NOT NULL;
ALTER TABLE reviews ALTER COLUMN student_engagement SET NOT NULL;
ALTER TABLE reviews ALTER COLUMN collaboration_teamwork SET NOT NULL;
ALTER TABLE reviews ALTER COLUMN behavior_management SET NOT NULL;
ALTER TABLE reviews ALTER COLUMN classroom_environment SET NOT NULL;
ALTER TABLE reviews ALTER COLUMN professional_ethics SET NOT NULL;
ALTER TABLE reviews ALTER COLUMN communication_skills SET NOT NULL;

ALTER TABLE reviews
    ADD CONSTRAINT ck_reviews_subject_matter_knowledge CHECK (subject_matter_knowledge BETWEEN 1 AND 5),
    ADD CONSTRAINT ck_reviews_teaching_methods CHECK (teaching_methods BETWEEN 1 AND 5),
    ADD CONSTRAINT ck_reviews_student_engagement CHECK (student_engagement BETWEEN 1 AND 5),
    ADD CONSTRAINT ck_reviews_collaboration_teamwork CHECK (collaboration_teamwork BETWEEN 1 AND 5),
    ADD CONSTRAINT ck_reviews_behavior_management CHECK (behavior_management BETWEEN 1 AND 5),
    ADD CONSTRAINT ck_reviews_classroom_environment CHECK (classroom_environment BETWEEN 1 AND 5),
    ADD CONSTRAINT ck_reviews_professional_ethics CHECK (professional_ethics BETWEEN 1 AND 5),
    ADD CONSTRAINT ck_reviews_communication_skills CHECK (communication_skills BETWEEN 1 AND 5);

CREATE INDEX IF NOT EXISTS idx_reviews_course_id ON reviews (course_id);

