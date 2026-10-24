# ShopAgent 首次 GitHub 上传核对

核对日期：2026-10-22。用户手动完成提交和上传，助手独立只读验证。

## 实际结果

- 仓库：git@github.com:wwzyhy/ShopAgent.git。
- 分支：main，已跟踪 origin/main。
- 首次提交：cf55862cf3019ef1644fb00f37709076058246a8。
- 提交说明：Initial ShopAgent demo: shopping, AI guide and local deployment。
- 本地 HEAD、本地 origin/main 与实时 git ls-remote 查询的 GitHub main 三者一致。
- 提交包含 288 个文件；33 项文档清单全部在该提交中。
- 检查时已跟踪文件的工作区改动为 0，暂存区改动为 0。
- 未包含约定的 secrets/backups/.venv/model_cache/target/node_modules/.taste-skill 目录、私密管理员配置或历史 provision-runtime 草稿路径。这是提交范围检查，不宣称全面安全审计。
- 其余历史教学资料、PDF、验证截图和本地协作记录继续留在电脑上。未跟踪文件存在不代表本次上传失败。

## 保存范围

本次保存了 Java/Python/前端源码、SQL结构脚本、部署配置、README关联文档和必要文本验收证据。没有上传运行数据库备份、真实密钥或模型缓存目录。

这是源码和文档归档，不代表网站已部署到公网，也不表示旧的全部学习材料已上传。docs/GIT_FIRST_UPLOAD.md 是首次提交之前准备的操作说明，其准备状态以本核对记录为后续结果。

本记录在首次上传核对完成后保存于本地，尚未另行提交或推送；可随下一次明确选择的文档更新归档。助手本轮没有代执行暂存、提交或推送，也没有运行服务、修改数据库或调用模型。

## 后续学习

回到现有功能复盘：先普通购物车 PUT /api/cart/items/{productId} → 当前用户 → 商品可用性 → 唯一键插入/更新 → 返回已保存数量，再接自然语言生成待确认操作和人工确认执行。不新增业务；Cookie/CSRF仍为独立暂缓主题。
