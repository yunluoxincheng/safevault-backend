-- 安全加固第三阶段：添加max_devices字段
-- 限制用户同时登录的设备数量

ALTER TABLE users ADD COLUMN max_devices INT NOT NULL DEFAULT 5;
ALTER TABLE users ADD COLUMN devices TEXT;

-- 添加注释
COMMENT ON COLUMN users.max_devices IS '最大同时登录设备数（默认5台）';
COMMENT ON COLUMN users.devices IS '登录设备数';
