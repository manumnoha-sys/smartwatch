from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")

    api_key: str
    database_url: str
    environment: str = "production"
    log_level: str = "INFO"


settings = Settings()
