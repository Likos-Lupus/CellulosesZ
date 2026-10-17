package top.likoslupus.cellulosesz.economy.config

import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Reads and writes an exact two-scale amount as a quoted decimal string. A TOML float is never
 * accepted on the money path, so an amount such as `12.50` must be written `"12.50"`.
 */
public object ExactMoneySerializer : KSerializer<BigDecimal> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "ExactMoney",
        PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(
            value.setScale(
                2,
                RoundingMode.UNNECESSARY
            ).toPlainString()
        )
    }

    override fun deserialize(decoder: Decoder): BigDecimal =
        BigDecimal(decoder.decodeString())
                .setScale(2, RoundingMode.UNNECESSARY)

}

/**
 * The native economy configuration. All amount inputs are exact decimal strings; display fields only
 * affect presentation and never rescale stored balances.
 */
@Serializable
public data class EconomySettings(
    public val enabled: Boolean = false,
    @SerialName("currency-name")
    public val currencyName: String = "Coins",
    @SerialName("currency-symbol")
    public val currencySymbol: String = "$",
    @SerialName("starting-balance")
    @Serializable(with = ExactMoneySerializer::class)
    public val startingBalance: BigDecimal = BigDecimal("0.00"),
    @SerialName("maximum-balance")
    @Serializable(with = ExactMoneySerializer::class)
    public val maximumBalance: BigDecimal = BigDecimal("1000000000000.00"),
    @SerialName("minimum-payment")
    @Serializable(with = ExactMoneySerializer::class)
    public val minimumPayment: BigDecimal = BigDecimal("0.01"),
    @SerialName("maximum-payment")
    @Serializable(with = ExactMoneySerializer::class)
    public val maximumPayment: BigDecimal = BigDecimal("1000000.00"),
    @SerialName("allow-offline-payments")
    public val allowOfflinePayments: Boolean = true,
    @SerialName("history-page-size")
    public val historyPageSize: Int = 10,
    @SerialName("balance-top-page-size")
    public val balanceTopPageSize: Int = 10,
    @SerialName("show-zero-balances-in-top")
    public val showZeroBalancesInTop: Boolean = false,
)
