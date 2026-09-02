# 成员协作指南

本文件是三名成员稳定的协作规范。开始任务前还要阅读 [AI 协作规则](AGENTS.md) 和 [每日任务看板](handoff.md)。技术任务使用 GitHub Issues/Projects 管理，每日分工与完成状态统一维护在 `handoff.md`。

## 1. 成员分工与共享文件

| 角色 | 主要职责 |
| --- | --- |
| 余（A）：后端架构 | 公共后端、用户、登录鉴权、地址、订单、测试规范 |
| 梁（B）：数据库与后端业务 | 数据库迁移、商家、店铺、分类、商品、购物车 |
| 龙（C）：前端与验收 | Vue、Router、Axios、公共组件、联调和验收材料 |

每个人都为自己负责的功能先写测试；A 维护测试规范，不包办所有测试。

共享内容指定主要负责人：

| 共享内容 | 主要负责人 |
| --- | --- |
| `pom.xml`、统一响应、异常处理、鉴权和后端公共配置 | A |
| 数据库迁移、初始化 SQL 和测试数据 | B |
| `package.json`、Router、Axios 和前端公共组件 | C |
| README、协作与验收文档 | A 汇总，其他成员提供内容 |

其他成员需要修改共享内容时，先在 Issue 或小组群说明，并邀请主要负责人审核。订单与购物车、用户与鉴权等跨模块功能必须先约定接口，不能直接修改他人模块内部实现。

## 2. 每日合并与 handoff 更新

- 每天 08:50 前，三人准备好昨天的 PR、测试结果和审核。
- 每天 09:00 统一合并已经完成、测试通过且满足审核要求的 PR；未完成任务不强行合并。
- 余是 `handoff.md` 的唯一维护人。成员完成任务后在群里发送 PR 和测试结果，由余更新状态，避免三个人同时修改同一个看板文件。
- 状态和 PR 编号更新到 `main` 后，三人重新拉取 `main`，按看板开始当天任务。
- 每人只修改看板分配给自己的板块；跨板块修改交给对应负责人处理或审核。
- 当前开发日、任务完成情况、PR 状态和阻塞等短时效信息只维护在 `handoff.md`；README、本文和接口契约只记录长期有效的信息，不重复当前进度。

## 3. GitHub 协作：PowerShell 傻瓜步骤

固定流程：

```text
创建或确认 Issue -> 同步 main -> 创建功能分支 -> 用 VS Code 写代码 -> 提交 -> push -> 创建 PR -> 队友审核 -> 合并
```

以下命令都在 PowerShell 中执行。命令成功后再进行下一步；不要下载 ZIP，不要再次运行 `git init`。

### 第一次使用：每台电脑只做一次

#### 1. 检查 Git 和 VS Code

```powershell
git --version
code --version
```

两个命令都应显示版本号。如果提示“无法识别”，先安装 [Git for Windows](https://git-scm.com/download/win) 和 [VS Code](https://code.visualstudio.com/)，安装完成后重新打开 PowerShell。

#### 2. 设置自己的提交身份

把引号中的内容替换为自己的 GitHub 用户名和 GitHub 绑定邮箱：

```powershell
git config --global user.name 你的GitHub用户名
git config --global user.email 你的GitHub邮箱
git config --global user.name
git config --global user.email
```

三个人必须使用各自的身份，不能共用组长的用户名和邮箱。

#### 3. 克隆项目

下面的命令会在当前用户目录下创建 `Projects` 文件夹并下载项目：

```powershell
$projectFolder = Join-Path $env:USERPROFILE Projects
New-Item -ItemType Directory -Force $projectFolder | Out-Null
Set-Location $projectFolder
git clone https://github.com/zhipeng-yu/elm-lite-platform.git
Set-Location .\elm-lite-platform
git status
code .
```

`git status` 应显示当前位于 `main`，工作区没有修改；`code .` 会用 VS Code 打开项目。

如果 GitHub 要求登录，按提示在浏览器中登录自己的 GitHub 账号并授权。不要在终端中输入 GitHub 网站密码；Windows 上的 Git Credential Manager 会在成功登录后保存凭据。参考 [GitHub 的 Windows 凭据说明](https://docs.github.com/en/get-started/git-basics/caching-your-github-credentials-in-git?platform=windows)。

### 每个新任务：所有人都重复执行

下面以“用户登录”任务为例。分支名必须换成自己的任务名称，例如：

```text
feature/zp-user-login
feature/ls-product-list
fix/ww-order-price
docs/zp-api-document
```

#### 0. 创建或确认 Issue

- 每个独立任务使用 `1 个 Issue → 1 个功能分支 → 1 个 PR`，开始写代码前先创建 Issue。
- 昨天未完成并顺延的同一任务继续使用原 Issue、原分支和原 PR，不重复创建。
- 同一任务中的多个提交共用一个 Issue；只有目标不同的新任务才创建新 Issue。
- 记住 Issue 编号，并在对应 PR 说明中填写 `Closes #编号`；PR 合并后 GitHub 会自动关闭该 Issue。

#### 1. 进入项目并同步 main

```powershell
Set-Location $env:USERPROFILE\Projects\elm-lite-platform
git switch main
git pull --ff-only
git status
```

只有看到工作区干净后才能开始新任务。如果 `git status` 显示有未提交修改，先处理原任务，不要继续执行。

#### 2. 创建自己的功能分支

把示例分支名替换为自己的：

```powershell
git switch -c feature/zp-user-login
git branch --show-current
code .
```

最后一个命令必须显示自己的功能分支，不能显示 `main`。同一个任务第二天继续时，不要重复创建分支，执行：

```powershell
git switch feature/zp-user-login
code .
```

#### 3. 在 VS Code 中写代码

只在自己的任务范围内修改文件，随时按 `Ctrl+S` 保存。后端接口和核心业务必须先写测试，再写实现。

写完测试后，先运行项目约定的测试命令，确认新增测试会因为功能尚未实现而失败，并保留结果。后端测试命令见 [README 的后端开发说明](readme.md#后端开发)。

然后在 VS Code 菜单选择“终端 → 新建终端”，执行：

```powershell
git status
git diff
git add .
git status
git diff --cached
```

确认暂存文件正确后执行：

```powershell
git commit -m 'test(user): define login behavior'
```

提交前查看 `git status` 列出的文件，确认没有 `.env`、密码、密钥、`node_modules` 或 `target`。

然后编写最小业务实现、运行测试。测试通过后执行：

```powershell
git status
git diff
git add .
git status
git diff --cached
git commit -m 'feat(user): implement login'
```

纯前端页面或文档任务不需要伪造失败测试，完成并验证后使用对应的 `feat` 或 `docs` 提交即可。

#### 4. 推送自己的分支

第一次推送该分支：

```powershell
git push -u origin feature/zp-user-login
```

以后继续向同一个分支推送：

```powershell
git push
```

永远不要执行 `git push --force`，也不要把自己的功能直接推送到 `main`。

#### 5. 在 GitHub 网页创建 Pull Request

1. 打开 <https://github.com/zhipeng-yu/elm-lite-platform>。
2. 点击黄色提示条中的 **Compare & pull request**；没有提示条时点击 **Pull requests → New pull request**。
3. 确认 `base` 是 `main`，`compare` 是自己的功能分支。
4. 填写标题和说明。
5. 普通组员必须在 Reviewers 中选择至少一名组员；仓库所有者自己的 PR 可以不选审核人。
6. 点击 **Create pull request**。普通组员等待审核，仓库所有者按 Ruleset 使用管理员 bypass 合并。

参考 [GitHub 官方 PR 创建步骤](https://docs.github.com/en/pull-requests/how-tos/create-pull-requests/creating-a-pull-request)。

PR 说明统一使用：

```text
完成：实现了什么
测试：执行了什么命令，结果是什么
影响：修改了哪些模块
AI：AI 用于什么，谁进行了人工复核
关联 Issue：Closes #编号
```

如果审核后还要修改，继续在原分支中改代码、提交并执行 `git push`。新提交会自动进入原 PR，不要新建第二个 PR。

#### 6. 队友审核和合并

审核人打开 PR：

1. 查看 **Files changed** 和测试说明。
2. 有问题就选择 **Request changes** 并留言。
3. 确认无误后选择 **Approve**。
4. 普通组员的 PR 在测试通过且至少一人批准后，使用 **Create a merge commit** 合并。

仓库所有者自己的 PR 不要求审核，但仍必须创建 PR，并通过 Ruleset 的 `Repository administrators / For pull requests only` bypass 合并。不要把 `Write role` 加入 bypass，否则所有组员都能跳过审核。

不要选择 **Squash and merge**，否则 `test` 和 `feat` 提交会被压成一个，无法证明 TDD 顺序。

#### 7. PR 合并后同步本地

把示例分支名替换为刚合并的分支：

```powershell
git switch main
git pull --ff-only
git branch -d feature/zp-user-login
git fetch --prune
git status
```

现在这个任务结束。开始下一个任务时，再从最新 `main` 创建新分支。

### 别人更新了 main，自己的任务还没完成

先保存并提交自己的修改，然后把最新 `main` 合并进功能分支：

```powershell
git status
git add .
git commit -m 'chore: save work before syncing main'
git fetch origin
git merge origin/main
```

没有冲突就运行测试并 `git push`。出现 `CONFLICT` 时立即停止，使用 VS Code 打开冲突文件，和修改同一文件的组员一起确认最终内容；不要盲目点击“全部接受”，不要执行 `git reset --hard`。

处理完成后执行：

```powershell
git add .
git commit -m 'chore: resolve merge conflicts'
git push
```

### 常见情况

#### 提示 `not a git repository`

说明 PowerShell 不在项目目录：

```powershell
Set-Location (Join-Path $env:USERPROFILE 'Projects\elm-lite-platform')
git status
```

#### 不小心在 main 上写了代码，但还没有提交

不要删除代码，立即创建功能分支，未提交修改会一起保留：

```powershell
git switch -c feature/你的名字-任务名
git status
```

#### push 被拒绝或不知道如何处理

不要强推、不要删除 `.git`、不要重新克隆覆盖本地文件。把完整报错发到小组群，由组员一起判断。

### 每次开发的最短检查表

- [ ] 从最新 `main` 创建了自己的功能分支。
- [ ] 已创建或确认当前任务的 Issue；顺延任务没有重复创建。
- [ ] `git branch --show-current` 显示的不是 `main`。
- [ ] 后端核心功能先提交测试，再提交实现。
- [ ] 提交前执行了 `git status` 和 `git diff`。
- [ ] push 的是自己的功能分支。
- [ ] PR 的 `base` 是 `main`，说明中包含 `Closes #编号`；普通组员已经邀请审核人。
- [ ] 测试通过后使用 merge commit 合并。
