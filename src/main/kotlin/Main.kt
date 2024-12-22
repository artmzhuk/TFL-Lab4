import Parser.*
import Tokenizer.Tokenizer


class CFG(private val groupsAst: Map<Int, Node>, private val root: Node) {
    private val NTs = mutableMapOf<Int, String>()
    private val counterMap = mutableMapOf<String, Int>()

    init {
        counterMap["CHAR"] = 0
        counterMap["NonCapt"] = 0
        counterMap["LookAh"] = 0
        counterMap["Concat"] = 0
        counterMap["Or"] = 0
        counterMap["KleeneSt"] = 0
        counterMap["G"] = 0
        counterMap["ELSE"] = 0
    }


    fun compute(): Map<String, List<List<String>>> {
        val rules = mutableMapOf<String, MutableList<List<String>>>()

        val resultNonTerminal = createCFG(root, rules)

        rules["S"] = mutableListOf(listOf(resultNonTerminal))


        return rules
    }

    private fun createCFG(
        node: Node,
        rules: MutableMap<String, MutableList<List<String>>>,
        nameNT: String? = null
    ): String {
        return when (node) {
            is CharNode -> {
                val nonTerminal = nameNT ?: createIndex("CHAR")
                rules.computeIfAbsent(nonTerminal) { mutableListOf() }.add(listOf(node.char.toString()))
                nonTerminal
            }

            is CaptureGroupNode -> {
                val nonTerminal = NTs.getOrPut(node.groupId) { "Group${node.groupId}" }
                val associated = createCFG(node.node, rules)
                rules.computeIfAbsent(nonTerminal) { mutableListOf() }.add(listOf(associated))
                nonTerminal
            }

            is NonCaptureGroupNode -> {

                val nonTerminal = nameNT ?: createIndex("NonCapt")
                val subNt = createCFG(node.node, rules)
                rules.computeIfAbsent(nonTerminal) { mutableListOf() }.add(listOf(subNt))
                nonTerminal
            }

            is LookaheadNode -> {
                val nonTerminal = nameNT ?: createIndex("LookAh")
                rules.computeIfAbsent(nonTerminal) { mutableListOf() }.add(emptyList()) // ε
                nonTerminal
            }

            is ConcatNode -> {
                val nonTerminal = nameNT ?: createIndex("Concat")
                val operandsNT = node.operands.map { createCFG(it, rules) }
                rules.computeIfAbsent(nonTerminal) { mutableListOf() }.add(operandsNT)
                nonTerminal
            }

            is OrNode -> {
                val nonTerminal = nameNT ?: createIndex("Or")
                for (operand in node.operands) {
                    val operandNt = createCFG(operand, rules)
                    rules.computeIfAbsent(nonTerminal) { mutableListOf() }.add(listOf(operandNt))
                }
                nonTerminal
            }

            is StarNode -> {
                val nonTerminal = nameNT ?: createIndex("KleeneSt")
                val subNt = createCFG(node.node, rules)
                rules.computeIfAbsent(nonTerminal) { mutableListOf() }.add(emptyList()) // ε
                rules[nonTerminal]!!.add(listOf(nonTerminal, subNt))
                nonTerminal
            }

            is ExprRefNode -> {
                val refId = node.refId
                if (refId !in NTs) {
                    val nonTerminal = "Group$refId"
                    NTs[refId] = nonTerminal
                    val ast = groupsAst[refId]
                        ?: throw RegexParserError("Группа захвата $refId не определена")
                    val subNt = createCFG(ast, rules)
                    rules.computeIfAbsent(nonTerminal) { mutableListOf() }.add(listOf(subNt))
                }
                NTs[refId]!!
            }

            else -> throw RegexParserError("Неизвестный тип")
        }
    }

    private fun createIndex(key: String): String{
        var idx = counterMap[key]
        if (idx != null) {
            counterMap.set(key, idx + 1)
            return key + idx
        } else {
            return key + counterMap["ELSE"]
        }
    }

}

fun printCFG(cfg: Map<String, List<List<String>>>){
    cfg.forEach { (nonTerminal, rightSide) ->
        rightSide.forEach { right ->
            val res = if (right.isEmpty()) "eps" else right.joinToString(" ")
            println("$nonTerminal --> $res")
        }
    }
}

fun main() {
    val input = readln()
    val tokenizer = Tokenizer(input)
    tokenizer.tokenize()
    val parser = RegexParser(tokenizer.tokens)
    val ast = parser.parse()
    val resCFG = CFG(parser.captureGroups, ast)
    val resCfg = resCFG.compute()
    println("КС-грамматика:")
    printCFG(resCfg)
    //println("*".repeat(100))
    //println(tokenizer.tokens)
    //println("*".repeat(100))
    //println(ast)
}