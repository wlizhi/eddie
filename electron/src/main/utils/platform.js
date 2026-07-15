/**
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 平台判断工具
 */

const IS_MAC = process.platform === 'darwin';
const IS_WIN = process.platform === 'win32';
const IS_LINUX = process.platform === 'linux';

const IS_STANDALONE = process.env.ELECTRON_STANDALONE === 'true';

module.exports = {
    IS_MAC,
    IS_WIN,
    IS_LINUX,
    IS_STANDALONE,
};
