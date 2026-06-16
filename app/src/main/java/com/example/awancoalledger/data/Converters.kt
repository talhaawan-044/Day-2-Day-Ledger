package com.example.awancoalledger.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromPartyType(value: PartyType): String = value.name

    @TypeConverter
    fun toPartyType(value: String): PartyType = PartyType.valueOf(value)

    @TypeConverter
    fun fromPaymentType(value: PaymentType): String = value.name

    @TypeConverter
    fun toPaymentType(value: String): PaymentType = PaymentType.valueOf(value)

    @TypeConverter
    fun fromExpenseCategory(value: ExpenseCategory): String = value.name

    @TypeConverter
    fun toExpenseCategory(value: String): ExpenseCategory = ExpenseCategory.valueOf(value)

    @TypeConverter
    fun fromReminderCategory(value: ReminderCategory): String = value.name

    @TypeConverter
    fun toReminderCategory(value: String): ReminderCategory = ReminderCategory.valueOf(value)

    @TypeConverter
    fun fromReminderPriority(value: ReminderPriority): String = value.name

    @TypeConverter
    fun toReminderPriority(value: String): ReminderPriority = ReminderPriority.valueOf(value)

    @TypeConverter
    fun fromReminderRecurrence(value: ReminderRecurrence): String = value.name

    @TypeConverter
    fun toReminderRecurrence(value: String): ReminderRecurrence = ReminderRecurrence.valueOf(value)
}
