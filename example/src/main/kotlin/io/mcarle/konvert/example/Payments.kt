package io.mcarle.konvert.example

import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Mapping

@Konverter
interface PaymentMapper {
    fun toDto(payment: Payment): PaymentDto

    @Konvert(mappings = [Mapping(source = "iban", target = "accountNumber")])
    fun toDto(transfer: Payment.Transfer): PaymentDto.Transfer
}

sealed interface Payment {
    data class Card(val last4: String) : Payment
    data class Transfer(val iban: String) : Payment
    data object Cash : Payment
    sealed interface Wallet : Payment {
        data class Paypal(val email: String) : Wallet
        data object ApplePay : Wallet
    }
}

sealed interface PaymentDto {
    data class Card(val last4: String) : PaymentDto
    data class Transfer(val accountNumber: String) : PaymentDto
    data object Cash : PaymentDto
    sealed interface Wallet : PaymentDto {
        data class Paypal(val email: String) : Wallet
        data object ApplePay : Wallet
    }
}
