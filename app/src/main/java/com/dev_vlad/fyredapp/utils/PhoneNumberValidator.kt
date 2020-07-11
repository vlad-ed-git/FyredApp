package com.dev_vlad.fyredapp.utils

object PhoneNumberValidator {
    private fun getCountryFromSelection(selection: String): String {
        return selection.substring(0, selection.indexOf('+')).trim { it <= ' ' }
    }

    private fun getCountryCodeFromSelection(selection: String): String {
        return selection.substring(selection.indexOf('+')).trim { it <= ' ' }
    }

    fun getPhoneNumberIfValid(
        countryCode: String?,
        phoneNumber: String?
    ): String? {
        if (phoneNumber == null) return null
        val phone = if (countryCode != null) countryCode + phoneNumber
        else phoneNumber
        val strippedPhone = removeNonNumericChars(phone)
        return if (strippedPhone.length < 9 || strippedPhone.length > 25 || !numeric(
                strippedPhone.substring(
                    1
                )
            )
        ) null else "+$strippedPhone"
    }


    private fun numeric(inputStr: String): Boolean {
        return try {
            inputStr.toDouble()
            true
        } catch (e: NumberFormatException) {
            false
        }
    }

    private fun removeNonNumericChars(string: String): String {
        return string.replace("[^\\d]".toRegex(), "")
    }

    fun getOnlyCountriesList(countryAndPhoneCodes: Array<String>): ArrayList<String> {
        val countryArrayList = ArrayList<String>()
        for (selection in countryAndPhoneCodes) {
            countryArrayList.add(getCountryFromSelection(selection))
        }
        return countryArrayList
    }

    fun getOnlyCodesList(countryAndPhoneCodes: Array<String>): ArrayList<String> {
        val codesArrayList = ArrayList<String>()
        for (selection in countryAndPhoneCodes) {
            codesArrayList.add(getCountryCodeFromSelection(selection))
        }
        return codesArrayList
    }

    fun addCountryCodeToPhoneNumber(countryCode: String?, phoneNumber: String?): String? {
        return when {
            phoneNumber == null -> null
            phoneNumber[0] == '+' -> getPhoneNumberIfValid(
                null,
                phoneNumber
            ) //no need to add country code
            else -> {
                getPhoneNumberIfValid(countryCode, phoneNumber) //add country code

            }
        }

    }
}