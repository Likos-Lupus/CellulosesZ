# CellulosesZ text writing guide

Long-term rules for player- and admin-visible text in `en_us` and `zh_cn`. The catalogs live at
`modules/<module>/src/main/resources/cellulosesz/i18n/<name>/messages_{en_us,zh_cn}.properties`.
Every change to a value must be reviewed in both languages; the key set and `{n}` placeholder set
must stay identical across languages (`verifyTranslations` enforces this).

## 1. Accuracy first

- Say what actually happened. Don't show "sent" before delivery, or "saved" when a write failed.
- Keep a separate warning when an operation partly succeeds: kit items delivered but the claim
  record failed, an action applied but the audit log failed. Never hide it inside a success line.
- Don't invent promises ("takes effect immediately", "permanent", "everyone will receive it") unless
  the code guarantees it. For config-dependent limits, say it depends on the server setting or give
  the documented default and label it "default".
- Player-authored content (chat, mail body, punishment reason) is shown verbatim. Never translate,
  decorate or re-parse it. Names, command literals, dimension IDs and permission nodes are not
  translated.

## 2. Voice and structure

- Lead with the concrete action and object. One short sentence is usually enough for success.
- Failure names the affected object and, when a fix is known, the next step. Don't blame the player,
  don't over-apologize, don't add filler ("Hope you enjoy!", "Sorry for the inconvenience").
- Different channels have different voices: ordinary commands, moderation, broadcasts and private
  messages. Use plain Minecraft-server language; no marketing, no fake cheer.
- Don't translate mechanically. `zh_cn` and `en_us` must state the same fact, permission meaning and
  consequence, but word order, plurals and punctuation may differ.

## 3. Simplified Chinese (zh_cn)

- Modern mainland Chinese, the way a server player reads it. Use "你", not "您". A moderator's
  feedback may be "已封禁 Alice", without the system referring to itself.
- Prefer everyday verbs: 已保存、找不到、暂时无法、还需等待. Avoid technical jargon such as 存储、连接端点、
  操作链路、已刷新请求、处于待处理状态.
- Use contrast structures ("不是……而是……" and similar) only for a real contrast. Avoid 本质上、值得注意的是、
  进一步来说、从某种意义上说、核心是、关键在于.
- Avoid职场/互联网黑话: 补一个、接一下、核一下、顺一下、拉起、落盘、兜底、闭环、收口、压实、口径、抽层.
- Avoid empty marketing words: 赋能、沉淀、深度、体系化、全方位、无缝、精准触达、统一感知、持续优化.
- Full-width Chinese punctuation ，。！？：；（）. Commands, IDs, code, parameters and file names stay
  ASCII. Keep list/detail punctuation consistent.

## 4. English (en_us)

- US spelling consistently: **canceled**, **armor**, **center**. Never mix cancelled/canceled.
- Direct verbs: Saved, Sent, Not found, "You can't…", "Try…". Match the real action: muted ≠ banned;
  removed ≠ deleted; a failed save is not a saved state.
- Never write `player(s)`, `message(s)`, `item(s)`, `moderator(s)`, `stack(s)` in player-facing
  text. Use a count label ("Recipients: {0}", "Players kicked: {0}") or a natural singular/plural
  form.
- "Private message" is `/msg`; "mail" is the stored, offline-capable system. Keep the distinction.
- Natural contractions are fine ("can't", "isn't"). Moderation stays neutral and matter-of-fact.
- Avoid AI/marketing vocabulary: delve, tapestry, testament, landscape, robust, seamless,
  comprehensive, leverage, harness, unlock, elevate, underscore, showcase, enhance. Avoid stock
  openings such as
  "It is important to note that", "Furthermore", "Moreover", "At its core", "Ultimately".

## 5. Sentence tone by scenario

| Scenario               | Write                                             | Don't write                              |
|------------------------|---------------------------------------------------|------------------------------------------|
| Ordinary success       | "已删除家 `base`。" / "Home `base` deleted."      | celebration, praise, internal flow       |
| Failure                | "找不到名为 `base` 的家。"                        | "请求执行失败" / "unknown error"         |
| Recoverable limit      | "还需等待 30 秒才能再次传送。"                    | "冷却中" with no wait                    |
| Async pending          | "正在保存邮件……"; skip pending for very fast work | a progress line for every trivial action |
| Moderation             | "已禁言 Alice 2 小时。"                           | emotional/insulting punishment language  |
| Notification           | "Alice 请求传送到你身边。接受 / 拒绝。"           | hiding who/which direction               |
| Private message / mail | source clear; body verbatim                       | rewriting the body, adding emoji         |
| Announcement           | keep a "公告" label; body verbatim                | marketing slogans around the body        |

## 6. Glossary (use these exactly)

| Concept / command       | zh_cn                                   | en_us                      | Constraint                               |
|-------------------------|-----------------------------------------|----------------------------|------------------------------------------|
| home / `/sethome`       | 家；设置家；保存家位置                  | home; set home             | not "家园"; not the server spawn         |
| warp                    | 公共传送点（上下文清楚时可简称 传送点） | warp                       | server-shared location                   |
| spawn                   | 服务器出生点（上下文清楚时 出生点）     | server spawn / spawn       | distinguish from the vanilla world spawn |
| `/tpa`, `/tpahere`      | 传送请求                                | teleport request           | state the direction                      |
| `/tpaccept` / `/tpdeny` | 接受 / 拒绝                             | Accept / Deny              | short button label                       |
| cooldown                | 冷却时间；还需等待……                    | cooldown; available in…    | say how long to wait                     |
| kit                     | 礼包                                    | kit                        | keep the player-familiar word            |
| once                    | 每人限领一次                            | one claim per player       | not "永久不能领取"                       |
| mail                    | 邮件                                    | mail                       | separate from instant chat               |
| msg / reply             | 私聊；回复                              | private message; reply     | body is the player's own words           |
| ignore                  | 忽略玩家；忽略名单                      | ignore; ignore list        | don't widen the scope                    |
| mute                    | 禁言                                    | mute                       | state the restricted channels            |
| ban / unban             | 封禁 / 解封                             | ban / unban                | IP ban must mention the address          |
| socialspy               | 私聊监听                                | private-message monitoring | not "社交监视"                           |
| vanish                  | 隐身                                    | vanish                     | don't promise full invisibility          |
| audit                   | 操作记录（面向管理员可用 审计记录）     | audit log                  | avoid for ordinary players               |
| HelpOp                  | 向管理员求助；管理求助消息              | staff help request         | source label may keep "HelpOp"           |
| broadcast               | 公告                                    | announcement               | body not translated                      |
| not found               | 找不到 / 没有记录                       | not found / no record      | distinguish "never seen" vs "not online" |
| unavailable             | 暂时无法使用 / 无法访问                 | unavailable                | name the real cause                      |
| dimension               | 维度                                    | dimension                  | keep IDs like `minecraft:overworld`      |
| item stack              | 一组物品                                | stack                      | amount vs type must not be swapped       |

Language names are shown localized ("简体中文", "English (US)"); parameters stay `zh_cn` / `en_us`.

## 7. Facts and wording

- "玩家不在线" = identity known but not online. "找不到玩家记录" = identity resolution failed; don't
  rewrite it as "玩家不存在".
- "权限不足" is only for permission denial; a protection rule says "此玩家受保护，无法执行该操作".
- "邮箱已满" is a capacity limit; keep "邮件功能关闭" and "邮件存储暂时无法访问" distinct.
- "传送请求已发送" does not mean the teleport happened; "已接受请求" is not automatically
  "传送成功".
- Kit item drops and claim-record failures keep their own warning.
