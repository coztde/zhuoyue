from pathlib import Path
import re

SRC = Path(r"C:\Users\33185\Desktop\test\output\AI大模型原理和应用面试题-156题整理.md")
BAK = SRC.with_suffix(".md.bak")


def block(conclusion: str, points: list[str], example: str, summary: str) -> str:
    summary = summary.removeprefix("面试可总结：")
    lines = [f"一句话结论：{conclusion}", "", "核心点："]
    for i, point in enumerate(points[:3], 1):
        lines.append(f"{i}. {point}")
    lines.extend(["", f"例子：{example}", "", f"面试总结：{summary}"])
    return "\n".join(lines)


def code_block(conclusion: str, points: list[str], code: str, summary: str) -> str:
    summary = summary.removeprefix("面试可总结：")
    lines = [f"一句话结论：{conclusion}", "", "核心点："]
    for i, point in enumerate(points[:3], 1):
        lines.append(f"{i}. {point}")
    lines.extend(["", "例子：", code.strip(), "", f"面试总结：{summary}"])
    return "\n".join(lines)


def contains_any(q: str, words: list[str]) -> bool:
    return any(word in q for word in words)


def generic_rag(q: str) -> str:
    focus = "检索链路的准确性和生成约束"
    if contains_any(q, ["流程", "完整", "文档处理"]):
        focus = "离线建库和在线问答两条主流程"
    elif contains_any(q, ["分块", "Embedding", "嵌入", "文档解析"]):
        focus = "知识入库质量"
    elif contains_any(q, ["扩展", "自查询", "Rerank", "混合检索", "优化", "调优"]):
        focus = "召回和排序优化"
    return block(
        "RAG 本质是先检索外部知识，再基于证据生成答案。",
        [
            f"核心工作不是只调用模型，而是把 {focus} 做稳。",
            "离线阶段要做清洗、切块、向量化、建索引；在线阶段要做检索、重排、组装上下文、生成答案。",
            "真正的目标是减少幻觉、补上私有知识和时效知识，而不是让模型更会“猜”。",
        ],
        "比如问公司报销规则，系统不是让模型凭记忆回答，而是先从制度库里找条款，再基于条款作答。",
        "面试可总结：RAG=外部知识库+检索+重排+受控生成，关键是证据找得准、给得稳。",
    )


def rerank() -> str:
    return block(
        "Rerank 本质是对初步召回结果做二次排序，目的是把真正相关的内容排到前面。",
        [
            "第一轮召回追求快，容易把“看起来相关但其实不准”的内容一起带回来。",
            "Rerank 会把 query 和候选文档成对判断相关度，精度通常高于纯向量相似度。",
            "工程上常见做法是 TopK 召回后再做 Rerank，最后只把最优的几段送给模型。",
        ],
        "比如问“离职补偿怎么算”，初召回会把“劳动合同解除”都捞出来，Rerank 会把真正讲补偿公式的内容排到最前面。",
        "面试可总结：Rerank 是用更贵但更准的方式，对召回结果做精排。",
    )


def a2a() -> str:
    return block(
        "A2A 协议本质是让不同 Agent 之间按统一协议协作。",
        [
            "它解决的是 Agent 到 Agent 的任务交接、状态同步和结果回传问题。",
            "设计上强调建立在成熟协议之上，比如 HTTP、流式传输、结构化消息，而不是重新发明底层通信。",
            "它特别适合多 Agent 分工场景，让总控 Agent 可以把子任务交给专业 Agent。",
        ],
        "比如旅行总控 Agent 负责拆任务，把订票交给票务 Agent，把酒店比价交给酒店 Agent，最后再汇总成一份方案。",
        "面试可总结：A2A 管的是 Agent 间协作，不是模型调用本身。",
    )


def mixed_retrieval() -> str:
    return block(
        "混合检索本质是把关键词检索和向量检索结合起来，互相补短板。",
        [
            "关键词检索擅长命中专有名词、编号、产品名，精确但不懂语义。",
            "向量检索擅长理解语义相近表达，懂意思但有时不够准。",
            "最终一般会做融合和重排，兼顾召回率和准确率。",
        ],
        "比如搜“iPhone15 电池健康规则”，关键词检索能命中 iPhone15，向量检索能补到“电池寿命”“健康度”这类近义表述。",
        "面试可总结：混合检索解决的是“只靠关键词不够懂、只靠向量不够准”的问题。",
    )


def structured_output() -> str:
    return block(
        "结构化输出就是让模型按指定的数据结构返回结果，而不是只返回一段自然语言。",
        [
            "常见格式是 JSON、对象数组、枚举值，方便程序直接消费。",
            "只靠提示词要求“请输出 JSON”稳定性不够，所以更推荐 schema 约束。",
            "它特别适合分类、抽取、表单回填、工单流转这类要和系统对接的场景。",
        ],
        "比如工单分类时，系统只需要 `{category, priority, reason}` 这样的结果，而不是一大段解释。",
        "面试可总结：结构化输出解决的是“模型能看懂，但程序不好接”的问题。",
    )


def langchain() -> str:
    return block(
        "LangChain 本质是 LLM 应用编排框架，用来把模型、提示词、检索、工具和记忆串成系统。",
        [
            "它的优势是组件化，能快速搭出问答、RAG、Agent 等能力。",
            "真正价值不在“黑盒更聪明”，而在把大模型应用开发工程化。",
            "复杂状态流通常会继续配合 LangGraph 这类图编排框架使用。",
        ],
        "比如一个文档问答系统里，LangChain 可以统一管理 Prompt、Retriever、ChatModel 和输出解析。",
        "面试可总结：LangChain 是编排层，不是替你解决所有效果问题的魔法框架。",
    )


def langgraph() -> str:
    return block(
        "LangGraph 本质是面向状态和流程控制的 Agent 编排框架。",
        [
            "它把流程拆成节点和边，节点负责动作，边负责流转。",
            "共享状态会在整个流程里持续传递，适合有分支、循环、人工介入的复杂任务。",
            "它常和 LangChain 组件配合使用：前者做图编排，后者做模型和工具组件。",
        ],
        "比如工单助手可以经过“分类 -> 检索 -> 回复 -> 人工复核”几个节点，每步都围绕同一份状态推进。",
        "面试可总结：LangGraph 的关键不是“图”，而是可控的状态化工作流。",
    )


def agent() -> str:
    return block(
        "Agent 本质是“模型负责推理，系统负责执行”的任务闭环。",
        [
            "它和单次问答的最大区别，是会围绕目标持续规划、调用工具、观察结果、再决定下一步。",
            "一个完整 Agent 至少要有模型、工具、记忆、执行循环和停止条件。",
            "上线时最重要的是权限边界、异常恢复和防死循环，而不只是让它更会说。",
        ],
        "比如普通问答只能告诉你“怎么订会议室”，Agent 可以直接查空闲时间、创建预约并通知参会人。",
        "面试可总结：Agent 的核心价值是完成任务，而不是把回答变长。",
    )


def vector_db() -> str:
    return block(
        "向量数据库本质是专门为高维向量相似度检索设计的数据库。",
        [
            "它解决的是海量 embedding 的高效存储和近似最近邻搜索问题。",
            "背后常用 HNSW、IVF、PQ 等索引结构，在速度和精度间取平衡。",
            "在大模型应用里，它最常见的角色就是支撑 RAG 的语义检索。",
        ],
        "比如企业有几十万段知识文本，用户提问后系统要在几百毫秒内找到最相关的几个 chunk，这就是向量库的价值。",
        "面试可总结：向量库不是为了存文本，而是为了又快又准地找相似语义。",
    )


def mcp() -> str:
    return block(
        "MCP 本质是让模型标准化接入工具、资源和外部系统的协议层。",
        [
            "它把工具和资源暴露方式统一下来，避免每接一个系统都重写一套对接逻辑。",
            "MCP 解决的不只是单次函数调用，还包括能力发现、资源读取、协议交互和结果回传。",
            "它特别适合企业内部把数据库、文件、搜索、业务 API 统一接给模型或 Agent 使用。",
        ],
        "比如把订单查询、日志检索、知识库搜索都封成 MCP Server，Agent 侧只按同一协议发现和调用即可。",
        "面试可总结：MCP 管的是能力接入标准，不是具体业务逻辑。",
    )


def finetune() -> str:
    return block(
        "微调本质是让通用模型更贴近特定业务、任务或风格要求。",
        [
            "预训练解决通用能力，微调解决业务适配和行为对齐。",
            "工程上常优先考虑 PEFT/LoRA，因为成本低、部署灵活、风险更可控。",
            "是否微调不能只看技术可行，还要看数据质量、评估方式和收益是否大于成本。",
        ],
        "比如客服场景要求模型必须按企业术语回答、按固定字段输出，这类需求单靠 Prompt 往往不够稳，微调会更合适。",
        "面试可总结：微调不是默认项，而是在 Prompt 和 RAG 不够稳时的增强手段。",
    )


def spring_ai() -> str:
    return block(
        "Spring AI 本质是把 AI 能力纳入 Spring 生态，用企业 Java 的方式做工程化落地。",
        [
            "它擅长统一模型接入、结构化输出、检索、Advisor、工具调用等能力。",
            "优势在于容易和 Spring Boot、配置中心、监控、安全体系、业务服务集成。",
            "适合做企业知识助手、工单助手、RAG、Tool Calling 这类典型后端 AI 应用。",
        ],
        "比如企业知识助手项目中，可以用 Spring AI 管聊天接口，再接向量库、MCP 工具和结构化输出。",
        "面试可总结：Spring AI 的价值是把 AI 应用从 Demo 拉到企业工程。",
    )


def prompt_answer() -> str:
    return block(
        "Prompt 设计的本质，是把任务边界、输入上下文和输出规则讲清楚，让模型稳定执行。",
        [
            "一个好 Prompt 至少要包含角色、任务、约束、输入变量、输出格式。",
            "复杂任务不要一句话全让模型自己猜，应该拆步骤、补规则、加失败兜底。",
            "Prompt 不是写得越花越好，越接近规格说明书越稳定。",
        ],
        "比如知识问答时，可以明确写“只基于给定资料回答；如果资料没有答案，就直接说不知道；最多三点”。",
        "面试可总结：好 Prompt 的核心不是技巧炫耀，而是边界清晰。",
    )


def computer_use() -> str:
    return block(
        "Computer Use 本质是让模型像人一样看界面、点按钮、输文本、操作软件。",
        [
            "它通常依赖截图理解、UI 元素定位、动作执行和结果反馈四个环节。",
            "这类能力适合没有 API 或 API 不完整的场景。",
            "真正难点不只是识别页面，而是保证动作稳定、安全、可回滚。",
        ],
        "比如让 Agent 在浏览器里登录系统、下载报表、再发邮件，这就是典型的 Computer Use 流程。",
        "面试可总结：Computer Use 是把 GUI 当成一种工具接口来使用。",
    )


def cot() -> str:
    return block(
        "CoT 思维链本质是让模型把中间推理步骤显式展开，从而提升多步推理稳定性。",
        [
            "它适合数学、逻辑、多条件判断这类需要分步思考的任务。",
            "实现方式可以是提示词引导，也可以给 few-shot 示例。",
            "生产里不一定要把完整思维链直接展示给用户，但内部推理过程很有价值。",
        ],
        "比如问“先打八折再减 30，一共多少钱”，让模型分步算通常比直接问结果更稳。",
        "面试可总结：CoT 的重点是按步骤想清楚，不是把回答写更长。",
    )


def openclaw() -> str:
    return block(
        "OpenClaw 本质是执行型 Agent 运行时，负责把多渠道消息、上下文、工具和 Agent Loop 串成闭环。",
        [
            "它关注的是工程落地：消息怎么接、会话怎么隔离、工具怎么调、上下文怎么控、插件怎么扩。",
            "核心价值不在于比别的模型更聪明，而在于让 Agent 能长期、稳定、可控地工作。",
            "面向复杂任务时，它通常会强调 Context Engine、Hook、插件、权限和幂等这类运行时能力。",
        ],
        "比如同一个 Agent 可以同时接 Telegram 和飞书消息，再调用相同的工具链完成任务，这就是运行时抽象带来的价值。",
        "面试可总结：OpenClaw 不是单纯聊天壳子，而是 Agent 的集成和运行框架。",
    )


def design_question(q: str) -> str:
    return block(
        "这类系统设计题，面试里最重要的是先拆链路，再说明 AI 负责哪一段、工程系统负责哪一段。",
        [
            "先把主流程讲清楚，比如数据进入、理解、检索、生成、校验、落库、反馈。",
            "再说明技术选型依据，比如用 RAG 还是微调、用规则还是模型、是否需要人工兜底。",
            "最后补上监控、评估、权限、延迟和成本这些上线必须考虑的工程点。",
        ],
        f"比如题目“{q}”，就可以按“输入 -> 处理 -> 输出 -> 风险控制”的顺序回答，避免一上来就堆术语。",
        "面试可总结：系统题先讲主流程，再讲 AI 作用点，最后讲工程兜底。",
    )


def default_answer(q: str) -> str:
    return block(
        "这题本质是在问一个 AI 能力如何从概念走到可落地实现。",
        [
            "回答时先说明它是干什么的，再讲一条主流程，不要一开始就堆细节。",
            "然后补 2 到 3 个关键点，比如准确率、成本、稳定性、权限边界。",
            "最后一定要给一个业务例子，这样回答更像真实项目经验。",
        ],
        f"比如像“{q}”这类题，只要先把本质和流程说清楚，面试官通常就能判断你是否真正理解。",
        "面试可总结：复杂题别散讲，抓住本质、流程、例子三件事。",
    )


def answer(idx: int, q: str) -> str:
    if idx == 3 or "Rerank" in q:
        return rerank()
    if contains_any(q, ["A2A"]):
        return a2a()
    if contains_any(q, ["混合检索"]):
        return mixed_retrieval()
    if contains_any(q, ["结构化输出", "Structured Outputs"]):
        return structured_output()
    if contains_any(q, ["LangGraph"]):
        return langgraph()
    if contains_any(q, ["LangChain", "LlamaIndex"]):
        return langchain()
    if contains_any(q, ["Computer Use"]):
        return computer_use()
    if contains_any(q, ["ReAct"]):
        return block(
            "ReAct 本质是边推理边行动，让模型在思考和工具调用之间形成闭环。",
            [
                "Reason 负责计划下一步，Act 负责执行动作，Observation 负责把结果带回上下文。",
                "它特别适合需要查询、操作、验证的任务，而不是只靠脑补就能回答的题。",
                "很多 Agent 框架的基础执行模式，本质上都是 ReAct 变体。",
            ],
            "比如用户问“帮我查上海明天是否下雨并给出穿衣建议”，模型会先调天气工具，再基于返回结果给建议。",
            "面试可总结：ReAct=推理+行动+观察，是 Agent 的经典闭环模式。",
        )
    if contains_any(q, ["CoT", "思维链"]):
        return cot()
    if contains_any(q, ["向量数据库", "HNSW", "LSH", "PQ", "ANN", "余弦相似度", "欧几里得", "曼哈顿"]):
        return vector_db()
    if contains_any(q, ["MCP", "Function Calling"]):
        return mcp()
    if contains_any(q, ["微调", "Fine-tuning", "PEFT", "LoRA", "SFT", "冻结层", "混合精度"]):
        return finetune()
    if contains_any(q, ["Spring AI"]):
        return spring_ai()
    if contains_any(q, ["Prompt", "提示"]):
        return prompt_answer()
    if contains_any(q, ["OpenClaw", "Context Engine", "Subagent", "Gateway", "Hook", "Skills", "插件", "幂等"]):
        return openclaw()
    if contains_any(q, ["Agent", "智能体", "Copilot", "记忆", "AutoGPT", "Manus", "OpenManus"]):
        return agent()
    if contains_any(q, ["RAG", "Embedding", "嵌入", "分块", "查询扩展", "自查询", "护栏", "GPTCache"]):
        return generic_rag(q)
    if contains_any(q, ["系统", "设计", "方案", "开发", "实现步骤", "性能", "稳定性", "测试", "评估", "场景"]):
        return design_question(q)
    return default_answer(q)


def answer_75() -> str:
    return code_block(
        "生成代码类 Prompt 的核心，是把技术栈、字段、校验规则和输出边界写成可执行规格。",
        [
            "不要只写“生成一个表单”，而要把字段、校验、错误提示、技术栈讲清楚。",
            "最好限制输出范围，比如是否只输出代码、是否包含样式、是否可直接运行。",
            "越接近真实开发约束，模型生成结果越稳定。",
        ],
        """```text
你是资深前端工程师，请使用 Vue3 + Composition API 生成一个“用户注册表单”组件。
要求：
1. 字段包含：用户名、手机号、邮箱、密码、确认密码。
2. 实现必填、长度、邮箱格式、两次密码一致校验。
3. 错误信息展示在对应表单项下方，提交前阻止非法数据提交。
4. 输出完整可运行代码，包含 template、script setup、style。
5. 关键逻辑写中文注释。
6. 只输出代码，不要解释。
```""",
        "面试可总结：Prompt 要写成规格说明，不要写成一句模糊需求。",
    )


def answer_76() -> str:
    return code_block(
        "带上下文约束的 Prompt，重点是把业务场景、技术边界和交互规则一次性讲清楚。",
        [
            "前端生成题要明确框架、语言、组件库、字段、校验和交互要求。",
            "把“只输出代码”“包含 import”“可直接运行”这种约束写死，结果会更稳。",
            "Prompt 越像接口文档，代码生成越接近工程要求。",
        ],
        """```text
你是高级 React 工程师，请生成一个可直接运行的 React 表单组件。
上下文约束：
- 技术栈：React 18 + TypeScript + Ant Design。
- 场景：新增员工信息表单。
- 字段：姓名、部门、手机号、邮箱、入职日期、状态。
- 校验：姓名必填；手机号为 11 位；邮箱格式校验；入职日期不能为空。
- 交互：提交时统一校验；错误提示显示在字段下方；提交按钮 loading；成功后回调 onSubmit。
- 输出要求：只输出完整代码，不要解释。
```""",
        "面试可总结：Prompt 设计得越像真实开发约束，生成结果越可用。",
    )


def rewrite() -> None:
    raw = SRC.read_text(encoding="utf-8")
    if not BAK.exists():
        BAK.write_text(raw, encoding="utf-8")
    source_text = BAK.read_text(encoding="utf-8") if BAK.exists() else raw
    questions = re.findall(r"## 第 (\d+) 题\n\n(.+?)\n\n解答：", source_text, re.S)
    if not questions:
        questions = re.findall(r"## 第 (\d+) 题\n\n\*\*题目：\*\* (.+?)\n\n\*\*标准回答：\*\*", raw, re.S)
    lines = [
        "# AI大模型原理和应用面试题（精细化整理版）",
        "",
        "> 说明：全文已统一为适合面试表达的固定模板，先讲本质结论，再讲关键点、例子和背诵总结。",
        "",
    ]
    for no, title in questions:
        idx = int(no)
        title = title.strip()
        if idx == 75:
            content = answer_75()
        elif idx == 76:
            content = answer_76()
        else:
            content = answer(idx, title)
        lines.extend(
            [
                f"## 第 {idx} 题",
                "",
                f"**题目：** {title}",
                "",
                "**标准回答：**",
                "",
                content,
                "",
            ]
        )
    SRC.write_text("\n".join(lines).rstrip() + "\n", encoding="utf-8")


if __name__ == "__main__":
    rewrite()
