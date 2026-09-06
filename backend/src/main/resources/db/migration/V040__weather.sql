-- ============================================================
-- V040__weather.sql
-- Living World Engine — Wetter pro Region
-- ============================================================
-- Jede Region bekommt dynamisches Wetter, das von der
-- Time Engine getrieben wird. Wetter beeinflusst NPC-Verhalten,
-- Sichtbarkeit und Bewegung.
--
-- Siehe docs/WORLD-DEPTH.md

CREATE TABLE region_weather (
    region_id       UUID         PRIMARY KEY REFERENCES regions(id) ON DELETE CASCADE,
    weather_type    VARCHAR(20)  NOT NULL DEFAULT 'CLEAR',
    temperature     INTEGER      NOT NULL DEFAULT 15,
    wind            INTEGER      NOT NULL DEFAULT 0,
    description     VARCHAR(200),
    changed_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_region_weather_changed ON region_weather(changed_at DESC);

COMMENT ON TABLE region_weather IS 'Aktuelles Wetter pro Region. Wird von WeatherService aktualisiert.';
COMMENT ON COLUMN region_weather.weather_type IS 'CLEAR, CLOUDY, RAIN, STORM, FOG, SNOW, EXTREME_HEAT';
COMMENT ON COLUMN region_weather.temperature IS 'In Celsius (-20 bis 50)';
COMMENT ON COLUMN region_weather.wind IS 'Windstärke 0-10';
COMMENT ON COLUMN region_weather.description IS 'Menschlich lesbare Beschreibung (z.B. "leichter Nieselregen")';
