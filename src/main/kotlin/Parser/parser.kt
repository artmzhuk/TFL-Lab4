package Parser

import Tokenizer.Token
import Tokenizer.TokenType

class CaptureGroupNode(val groupId: Int, val node: Node) : Node() {
    override fun toString(): String = "GroupNode{$groupId, $node}"
}

class NonCaptureGroupNode(val node: Node) : Node() {
    override fun toString(): String = "NonCaptureGroupNode{$node}"
}

class LookaheadNode(val node: Node) : Node() {
    override fun toString(): String = "LookaheadNode{$node}"
}

class ConcatNode(val operands: List<Node>) : Node() {
    override fun toString(): String = "ConcatNode{$operands}"
}

class OrNode(val operands: List<Node>) : Node() {
    override fun toString(): String = "AltNode{$operands}"
}

class StarNode(val node: Node) : Node() {
    override fun toString(): String = "StarNode{$node}"
}

class CharNode(val char: Char) : Node() {
    override fun toString(): String = "CharNode{'$char'}"
}

class ExprRefNode(val refId: Int) : Node() {
    override fun toString(): String = "ExprRefNode{$refId}"
}

abstract class Node
class RegexParser(private val tokens: List<Token>) {
    private var pos = 0
    private var groupIdx = 0
    private var isInLookahead = false
    val captureGroups = mutableMapOf<Int, Node>()

    private val currentToken: Token?
        get() = tokens.getOrNull(pos)

    private fun advance(expectedType: TokenType): Token {
        val token = currentToken ?: throw RegexParserError("Ошибка парсинга, неожиданный EOF")
        if (token.type != expectedType) {
            throw RegexParserError("Ожидалось $expectedType, встретилось ${token.type}")
        }
        pos++
        return token
    }

    fun parse(): Node {
        val root = parseOr()
        if (currentToken != null) {
            throw RegexParserError("После валидного парсинга остались лишние токены: $currentToken")
        }
        return root
    }

    private fun parseOr(): Node {
        val operands = mutableListOf<Node>()
        val firstOperand = parseConcatenation()
        operands.add(firstOperand)
        while (currentToken?.type == TokenType.OR) {
            advance(TokenType.OR)
            if (currentToken == null || currentToken?.type in listOf(TokenType.BRACKET_CLOSE, TokenType.OR)) {
                throw RegexParserError("'|' без операндов не допускается")
            }
            operands.add(parseConcatenation())
        }
        return if (operands.size == 1) firstOperand else OrNode(operands)
    }

    private fun parseConcatenation(): Node {
        val nodes = mutableListOf<Node>()
        while (currentToken != null && currentToken?.type !in listOf(TokenType.BRACKET_CLOSE, TokenType.OR)) {
            nodes.add(parseKleene())
        }
        return if (nodes.size == 1) nodes[0] else ConcatNode(nodes)
    }

    private fun parseKleene(): Node {
        var node = parseRG()
        while (currentToken?.type == TokenType.KLEENE_STAR) {
            advance(TokenType.KLEENE_STAR)
            node = StarNode(node)
        }
        return node
    }

    private fun parseRG(): Node {
        val token = currentToken ?: throw RegexParserError("Неожиданный EOF при парсинге")

        return when (token.type) {
            TokenType.CAPTURE_START -> {
                if (isInLookahead) {
                    throw RegexParserError("В lookahead встретилась группа захвата")
                }
                advance(TokenType.CAPTURE_START)
                groupIdx++
                if (groupIdx > 9) {
                    throw RegexParserError("Кол-во групп захвата больше 9")
                }
                val groupNumber = groupIdx
                val node = parseOr()
                advance(TokenType.BRACKET_CLOSE)
                captureGroups[groupNumber] = node
                CaptureGroupNode(groupNumber, node)
            }

            TokenType.NONCAPTURE_START -> {
                advance(TokenType.NONCAPTURE_START)
                val node = parseOr()
                advance(TokenType.BRACKET_CLOSE)
                NonCaptureGroupNode(node)
            }

            TokenType.LOOKAHEAD_START -> {
                if (isInLookahead) {
                    throw RegexParserError("В lookahead встретился lookahead")
                }
                advance(TokenType.LOOKAHEAD_START)
                isInLookahead = true
                val node = parseOr()
                advance(TokenType.BRACKET_CLOSE)
                isInLookahead = false
                LookaheadNode(node)
            }

            TokenType.REFERENCE_START -> {
                advance(TokenType.REFERENCE_START)
                advance(TokenType.BRACKET_CLOSE)
                ExprRefNode((token.value as Char).digitToInt())
            }

            TokenType.CHAR -> {
                val ch = token.value as Char
                advance(TokenType.CHAR)
                CharNode(ch)
            }

            else -> throw RegexParserError("Несуществующий токен: $token")
        }
    }

}

class RegexParserError(message: String) : RuntimeException(message)