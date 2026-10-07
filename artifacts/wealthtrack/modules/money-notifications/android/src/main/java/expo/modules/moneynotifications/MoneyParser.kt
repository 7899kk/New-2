package expo.modules.moneynotifications

// Pure parser: no Android dependencies, network, OTPs, or raw notification retention.
data class MoneyEvent(val amount: Double, val kind: String, val reference: String?)
object MoneyParser {
  private val money = Regex("(?:₹|INR\\s*|Rs\\.?\\s*)([0-9][0-9,]*(?:\\.[0-9]{1,2})?)(?![0-9,]|\\.[0-9])", RegexOption.IGNORE_CASE)
  fun parse(text: String): MoneyEvent? {
    if (Regex("\\b(otp|one.time.password|verification.code|authentication.code|promo|offer|cashback.offer)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)) return null
    if (Regex("\\b(failed|declined|pending|processing|requested|request|request.to.pay|due|reminder|mandate|scheduled)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)) return null
    val amounts = money.findAll(text).mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }.toList()
    if (amounts.isEmpty()) return null
    // Multiple currency amounts may include an account balance: review rather than guess.
    val amount = amounts.first()
    if (!amount.isFinite() || amount <= 0.0 || amount > 1_000_000_000.0) return null
    val debit = Regex("\\b(debited|paid|sent|payment.successful|payment.completed)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
    val credit = Regex("\\b(credited|received)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
    val refund = Regex("\\b(refund|refunded|reversal|reversed|cashback)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
    val transfer = Regex("\\b(self.transfer|own.account|between.your.accounts)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
    val ref = Regex("(?:UTR|UPI\\s*Ref(?:erence)?|Txn(?:\\s*ID)?|Transaction\\s*ID|Ref(?:erence)?)\\s*[:#.-]?\\s*([A-Za-z0-9]{6,40})", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.uppercase()
    val kind = when {
      transfer -> "transfer"
      refund -> "review"
      amounts.size != 1 || debit == credit || ref == null -> "review"
      debit -> "expense"
      else -> "income"
    }
    return MoneyEvent(amount, kind, ref)
  }
}
