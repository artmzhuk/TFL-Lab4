package Tokenizer

data class Token(val type: TokenType, val value: Any? = null)
enum class TokenType {
    CAPTURE_START, NONCAPTURE_START, LOOKAHEAD_START, REFERENCE_START, CHAR, OR, KLEENE_STAR, BRACKET_CLOSE
}

class Tokenizer(input: String) {
    private val input = input
    private var pos = 0
    private val inputLength = input.length
    val tokens = mutableListOf<Token>()

    init {
        if (inputLength < 1) {
            throw Error("Строка пустая")
        }
    }

    fun peek(): Char {
        if (pos < inputLength) {
            return input[pos]
        } else {
            throw Error("При токенизации встретился неожиданный EOF")
        }
    }

    fun advance() {
        pos++
    }

    fun tokenize() {
        while (pos < inputLength) {
            if (peek() == '(') {
                advance()
                if (peek() == '?') {
                    advance()
                    if (peek() == ':') {
                        tokens.add(Token(TokenType.NONCAPTURE_START, ' '))
                        advance()
                    } else if (peek() == '=') {
                        tokens.add(Token(TokenType.LOOKAHEAD_START, ' '))
                        advance()
                    } else {
                        if (peek().isDigit()) {
                            tokens.add(Token(TokenType.REFERENCE_START, peek()))
                            advance()
                        } else {
                            throw Error("При токенизации возникла ошибка, после ? встретилось" + peek())
                        }
                    }
                } else {
                    tokens.add(Token(TokenType.CAPTURE_START, ' '))
                    //не продвигаемся тк заходим в ( [rg] )
                    //                             ^
                }
            } else if (peek() == '|') {
                tokens.add(Token(TokenType.OR, ' '))
                advance()
            } else if (peek() == '*') {
                tokens.add(Token(TokenType.KLEENE_STAR, ' '))
                advance()
            } else if (peek().isLetter() && peek().isLowerCase()) {
                tokens.add(Token(TokenType.CHAR, peek()))
                advance()
            } else if (peek() == ')') {
                tokens.add(Token(TokenType.BRACKET_CLOSE, ' '))
                advance()
            } else {
                throw Error("Ошибка токенизации: встретилось " +  peek())
            }
        }
    }
}