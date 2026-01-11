-- Zero Knowledge Architecture: Redesign users table for email-based authentication
-- This migration adds support for zero-knowledge encryption architecture

-- Add new columns to users table for zero-knowledge authentication
ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(255) UNIQUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_verifier CHAR(88);
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_salt CHAR(44);
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_iterations INT NOT NULL DEFAULT 100000;

-- Add columns for encrypted master key (for biometric unlock)
ALTER TABLE users ADD COLUMN IF NOT EXISTS encrypted_master_key TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS master_key_iv CHAR(24);

-- Add column for encrypted RSA private key (for password sharing)
ALTER TABLE users ADD COLUMN IF NOT EXISTS private_key_encrypted TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS private_key_iv CHAR(24);

-- Add column for key hash (for quick verification)
ALTER TABLE users ADD COLUMN IF NOT EXISTS key_hash CHAR(88);

-- Make device_id nullable since we're moving to email-based auth
ALTER TABLE users ALTER COLUMN device_id DROP NOT NULL;
ALTER TABLE users ALTER COLUMN device_id DROP DEFAULT;

-- Create index on email
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Create index for users who have completed zero-knowledge setup
CREATE INDEX IF NOT EXISTS idx_users_has_verifier ON users(password_verifier) WHERE password_verifier IS NOT NULL;

-- Add comment for documentation
COMMENT ON COLUMN users.email IS 'User email for login (zero-knowledge auth)';
COMMENT ON COLUMN users.password_verifier IS 'PBKDF2-derived verifier for master password validation';
COMMENT ON COLUMN users.password_salt IS 'Salt used for PBKDF2 key derivation';
COMMENT ON COLUMN users.password_iterations IS 'Number of PBKDF2 iterations (default: 100000)';
COMMENT ON COLUMN users.encrypted_master_key IS 'Encrypted master key for biometric unlock (optional)';
COMMENT ON COLUMN users.master_key_iv IS 'IV for encrypted master key';
COMMENT ON COLUMN users.private_key_encrypted IS 'Encrypted RSA private key for password sharing';
COMMENT ON COLUMN users.private_key_iv IS 'IV for encrypted private key';
COMMENT ON COLUMN users.key_hash IS 'Hash of master key for quick verification';
