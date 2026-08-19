CREATE TABLE catalog_content_translations (
    content_id UUID NOT NULL REFERENCES catalog_contents(id) ON DELETE CASCADE,
    language_code VARCHAR(10) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    cover_alternative_text VARCHAR(500),
    PRIMARY KEY (content_id, language_code),
    CONSTRAINT ck_catalog_content_translation_language CHECK (language_code IN ('en')),
    CONSTRAINT ck_catalog_content_translation_title CHECK (length(trim(title)) > 0)
);

CREATE TABLE catalog_season_translations (
    season_id UUID NOT NULL REFERENCES catalog_seasons(id) ON DELETE CASCADE,
    language_code VARCHAR(10) NOT NULL,
    title VARCHAR(200) NOT NULL,
    PRIMARY KEY (season_id, language_code),
    CONSTRAINT ck_catalog_season_translation_language CHECK (language_code IN ('en')),
    CONSTRAINT ck_catalog_season_translation_title CHECK (length(trim(title)) > 0)
);

CREATE TABLE catalog_episode_translations (
    episode_id UUID NOT NULL REFERENCES catalog_episodes(id) ON DELETE CASCADE,
    language_code VARCHAR(10) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    PRIMARY KEY (episode_id, language_code),
    CONSTRAINT ck_catalog_episode_translation_language CHECK (language_code IN ('en')),
    CONSTRAINT ck_catalog_episode_translation_title CHECK (length(trim(title)) > 0)
);

CREATE TABLE quiz_version_translations (
    quiz_version_id UUID NOT NULL REFERENCES quiz_versions(id) ON DELETE CASCADE,
    language_code VARCHAR(10) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    fallback_alternative_text VARCHAR(500),
    PRIMARY KEY (quiz_version_id, language_code),
    CONSTRAINT ck_quiz_version_translation_language CHECK (language_code IN ('en')),
    CONSTRAINT ck_quiz_version_translation_title CHECK (length(trim(title)) > 0)
);

CREATE TABLE quiz_question_translations (
    question_id UUID NOT NULL REFERENCES quiz_questions(id) ON DELETE CASCADE,
    language_code VARCHAR(10) NOT NULL,
    prompt VARCHAR(1000) NOT NULL,
    visual_alternative_text VARCHAR(500),
    accessible_prompt VARCHAR(1000),
    PRIMARY KEY (question_id, language_code),
    CONSTRAINT ck_quiz_question_translation_language CHECK (language_code IN ('en')),
    CONSTRAINT ck_quiz_question_translation_prompt CHECK (length(trim(prompt)) > 0)
);

CREATE TABLE quiz_answer_option_translations (
    answer_option_id UUID NOT NULL REFERENCES quiz_answer_options(id) ON DELETE CASCADE,
    language_code VARCHAR(10) NOT NULL,
    option_text VARCHAR(500) NOT NULL,
    PRIMARY KEY (answer_option_id, language_code),
    CONSTRAINT ck_quiz_option_translation_language CHECK (language_code IN ('en')),
    CONSTRAINT ck_quiz_option_translation_text CHECK (length(trim(option_text)) > 0)
);

-- Known catalogue copy is seeded idempotently by stable Turkish source text. Existing
-- installations receive the English projection without changing source identities.
INSERT INTO catalog_content_translations (content_id, language_code, title, description, cover_alternative_text)
SELECT id, 'en',
       CASE title
           WHEN 'Gassal' THEN 'The Mortician'
           WHEN 'Siyah Bere' THEN 'Black Beret'
           WHEN 'Persona' THEN 'Persona'
           WHEN 'İmam Gazâlî' THEN 'Imam al-Ghazali'
           WHEN 'Büyük Geri Dönüş' THEN 'The Comeback Trail'
           WHEN 'Mogadişu''dan Kaçış' THEN 'Escape from Mogadishu'
           WHEN 'ROCKY' THEN 'ROCKY'
       END,
       CASE title
           WHEN 'Gassal' THEN 'After coming face to face with death, mortician Baki decides to make changes in his life.'
           WHEN 'Siyah Bere' THEN 'Separated from their battalion, a tank crew defies death to find their way home.'
           WHEN 'Persona' THEN 'A series of murders mirrors the cases in criminal psychology expert Yiğit Dağlı''s book.'
           WHEN 'İmam Gazâlî' THEN 'The story of al-Ghazali''s difficult journey as he questions knowledge and truth.'
           WHEN 'Büyük Geri Dönüş' THEN 'A debt-ridden Hollywood producer plans a dangerous film in order to collect the insurance money.'
           WHEN 'Mogadişu''dan Kaçış' THEN 'During Somalia''s civil war, the staff of two embassies try to escape Mogadishu.'
           WHEN 'ROCKY' THEN 'Rocky Balboa, an amateur boxer in Philadelphia, gets an unexpected shot at the heavyweight championship and sets out to prove to himself and the world just how good a fighter he is.'
       END,
       CASE WHEN cover_media_id IS NULL THEN NULL ELSE
           CASE WHEN title = 'ROCKY' THEN 'Rocky film cover' ELSE
               (CASE title
                   WHEN 'Gassal' THEN 'The Mortician'
                   WHEN 'Siyah Bere' THEN 'Black Beret'
                   WHEN 'Persona' THEN 'Persona'
                   WHEN 'İmam Gazâlî' THEN 'Imam al-Ghazali'
                   WHEN 'Büyük Geri Dönüş' THEN 'The Comeback Trail'
                   WHEN 'Mogadişu''dan Kaçış' THEN 'Escape from Mogadishu'
               END) || ' cover image'
           END
       END
FROM catalog_contents
WHERE title IN ('Gassal', 'Siyah Bere', 'Persona', 'İmam Gazâlî', 'Büyük Geri Dönüş', 'Mogadişu''dan Kaçış', 'ROCKY')
ON CONFLICT (content_id, language_code) DO NOTHING;

INSERT INTO quiz_version_translations (quiz_version_id, language_code, title, description, fallback_alternative_text)
SELECT id, 'en', 'Could Rocky take you in a street fight?',
       'How well do you remember the details of Rocky?',
       CASE WHEN fallback_media_id IS NULL THEN NULL ELSE 'Rocky quiz image' END
FROM quiz_versions
WHERE title = 'Rocky sokak kavgasında seni alabilir mi?'
ON CONFLICT (quiz_version_id, language_code) DO NOTHING;

INSERT INTO quiz_question_translations (question_id, language_code, prompt, visual_alternative_text, accessible_prompt)
SELECT id, 'en',
       'After his opponent is injured, why does World Heavyweight Champion Apollo Creed specifically choose Rocky Balboa from Philadelphia for the title fight?',
       CASE WHEN visual_alternative_text IS NULL THEN NULL ELSE 'A scene related to Apollo Creed choosing Rocky Balboa as his opponent' END,
       CASE WHEN accessible_prompt IS NULL THEN NULL ELSE 'Apollo wants to tie the fight to the 1976 Bicentennial celebrations rather than present it as a purely sporting rivalry. The “Italian Stallion” nickname and the chance given to an unknown immigrant kid make perfect publicity for an American Dream story.' END
FROM quiz_questions
WHERE prompt = 'Rakibi sakatlanan Dünya Ağır Sıklet Şampiyonu Apollo Creed, unvan maçı için Philly''den neden özellikle Rocky Balboa''yı seçer?'
ON CONFLICT (question_id, language_code) DO NOTHING;

INSERT INTO quiz_answer_option_translations (answer_option_id, language_code, option_text)
SELECT id, 'en', CASE option_text
    WHEN 'Rocky''nin geçmişteki maç kayıtlarının ve nakavt istatistiklerinin şampiyona layık olması' THEN 'Rocky''s past fight record and knockout statistics make him worthy of a championship bout'
    WHEN 'Amerika''nın 200. yılında "fırsatlar ülkesi" temasını İtalyan Aygırı lakabıyla pazarlamak istemesi' THEN 'He wants to market America as the “land of opportunity” during its Bicentennial by using Rocky''s Italian Stallion nickname'
    WHEN 'Philadelphia boks komisyonunun yerel bir İtalyan boksörle dövüşmesi için yasal baskı yapması' THEN 'The Philadelphia boxing commission legally pressures him to fight a local Italian boxer'
    WHEN 'Rocky''nin menajeri Mickey''nin ulusal televizyona çıkıp Apollo''yu açıkça korkaklıkla suçlaması' THEN 'Rocky''s manager Mickey goes on national television and publicly calls Apollo a coward'
END
FROM quiz_answer_options
WHERE option_text IN (
    'Rocky''nin geçmişteki maç kayıtlarının ve nakavt istatistiklerinin şampiyona layık olması',
    'Amerika''nın 200. yılında "fırsatlar ülkesi" temasını İtalyan Aygırı lakabıyla pazarlamak istemesi',
    'Philadelphia boks komisyonunun yerel bir İtalyan boksörle dövüşmesi için yasal baskı yapması',
    'Rocky''nin menajeri Mickey''nin ulusal televizyona çıkıp Apollo''yu açıkça korkaklıkla suçlaması'
)
ON CONFLICT (answer_option_id, language_code) DO NOTHING;
