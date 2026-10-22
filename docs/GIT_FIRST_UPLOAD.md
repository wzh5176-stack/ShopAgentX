# ShopAgent 首次 GitHub 归档

当前步骤：用户手动暂存、提交与推送；助手集中检查。本文不是已上传证明。

## 已核实（2026-10-22）

- 本地分支 main，尚无提交。
- SSH 认证账号 wwzyhy；origin 为 git@github.com:wwzyhy/ShopAgent.git，远端引用查询成功且没有分支或标签。
- 程序主体 255 个文件已由用户暂存；不含 deploy/scripts/provision-runtime.py。
- 忽略规则已覆盖 secrets/、backups/、.taste-skill/、环境及模型缓存等。
- 补充清单列出 33 个文档文件：README 的本地链接关联文档与必要证据，以及主路线、环境说明和本次上传说明。
- 对所选的 31 份既有文本进行了常见 API Token、私钥、密码散列及认证头模式检查，未发现命中；这不能保证检测出所有秘密。
- 历史 PDF、界面截图、旧验证脚本与本地协作状态暂不加入首批。文件继续保留，不删除。
- 未执行提交或推送；未操作网站服务、数据库或模型。

## 1. 补齐文档

在 Windows PowerShell 执行：

```powershell
Set-Location "D:\ShopAgent-X"
git add --pathspec-from-file=docs/FIRST_COMMIT_FILES.txt
git diff --cached --shortstat
```

清单里每行是一个仓库相对文件路径，Git 只补入列出的文件；此前暂存的程序主体继续保留。预期共 288 个新增文件。若清单与预期不同，先核对差异，再提交。

## 2. 保存第一版

```powershell
git commit -m "Initial ShopAgent demo: shopping, AI guide and local deployment"
git log -1 --oneline
```

commit 把暂存内容保存为本地版本，会显示一串提交编号。它不上传到 GitHub。若提示错误，先解决错误，不跳到下一步。

## 3. 上传 GitHub

确认提交成功后执行：

```powershell
git push -u origin main
```

origin 是已配置的目标仓库，main 是本地分支；-u 设置对应关系，后续可使用 git push。正常初次上传应显示 main -> main 和跟踪关系。不使用强制推送。

## 4. 交付检查

用户完成后由助手只读核对：

- HEAD 提交编号与 origin/main、GitHub 远端 main 是否一致。
- README 及本次选择的文档是否已在提交中。
- 私密目录、备份、虚拟环境与模型缓存没有被提交。
- 未收录的本地学习材料属于本次明确保留范围；git status 仍出现这些未跟踪文件不代表上传失败。
- GitHub 归档是保存源码与文档，不等于公网部署网站。

完成后再记录实际提交编号、推送结果与远端一致性，不能提前记成成功。
