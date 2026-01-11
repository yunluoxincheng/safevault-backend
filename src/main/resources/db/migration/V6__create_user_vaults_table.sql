-- Zero Knowledge Architecture: Create user_vaults table for encrypted password vault storage

-- Create user vaults table
CREATE TABLE IF NOT EXISTS user_vaults (
    vault_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    encrypted_data TEXT NOT NULL,
    data_iv CHAR(24) NOT NULL,
    data_auth_tag CHAR(32),
    version BIGINT NOT NULL DEFAULT 0,
    last_synced_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_vaults_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT uq_user_vaults_user UNIQUE (user_id)
);

-- Create index on user_id for fast lookup
CREATE INDEX IF NOT EXISTS idx_user_vaults_user_id ON user_vaults(user_id);

-- Create index on version for conflict detection
CREATE INDEX IF NOT EXISTS idx_user_vaults_version ON user_vaults(version);

-- Create trigger to update updated_at
CREATE TRIGGER update_user_vaults_updated_at
    BEFORE UPDATE ON user_vaults
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE user_vaults IS 'Stores encrypted password vaults for each user';
COMMENT ON COLUMN user_vaults.vault_id IS 'Unique vault identifier';
COMMENT ON COLUMN user_vaults.user_id IS 'Owner of the vault';
COMMENT ON COLUMN user_vaults.encrypted_data IS 'AES-256-GCM encrypted vault data (Base64)';
COMMENT ON COLUMN user_vaults.data_iv IS 'Initialization vector for encryption (Base64)';
COMMENT ON COLUMN user_vaults.data_auth_tag IS 'GCM authentication tag (Base64)';
COMMENT ON COLUMN user_vaults.version IS 'Version number for conflict detection and sync';
COMMENT ON COLUMN user_vaults.last_synced_at IS 'Last time the vault was synced with server';
