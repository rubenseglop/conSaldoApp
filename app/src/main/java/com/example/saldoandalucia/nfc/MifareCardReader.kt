package com.example.saldoandalucia.nfc

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Lector de tarjetas Mifare Classic 1K del Consorcio de Transporte de Andalucía.
 *
 * Según la documentación pública del proyecto SAND, la información relevante
 * está en los sectores 8 y 9:
 *  - Sector 8, bloque 34: datos del contrato (tipo de tarjeta).
 *  - Sector 9, bloque 36: copia de seguridad del contrato.
 *  - Sector 9, bloque 37: saldo (Value Block), Little Endian, 1 unidad = 0.005€.
 *
 * Uso exclusivamente educativo: solo debes leer/escribir tarjetas de tu propiedad.
 */
class MifareCardReader(private val tag: Tag) {

    companion object {
        private const val SECTOR_CONTRACT = 8
        private const val SECTOR_BALANCE = 9
        private const val BLOCK_CONTRACT_INDEX_IN_SECTOR = 2   // bloque 34 (sector 8, bloque relativo 2)
        private const val BLOCK_BALANCE_INDEX_IN_SECTOR = 1    // bloque 37 (sector 9, bloque relativo 1)

        private val KEY_A_SECTOR_8 = hexStringToByteArray("51B3EF60BF56")
        private val KEY_A_SECTOR_9 = hexStringToByteArray("99100225D83B")

        private const val UNITS_TO_EURO = 0.005

        private fun hexStringToByteArray(hex: String): ByteArray {
            val clean = hex.replace(" ", "")
            val result = ByteArray(clean.length / 2)
            for (i in result.indices) {
                val index = i * 2
                result[i] = clean.substring(index, index + 2).toInt(16).toByte()
            }
            return result
        }
    }

    data class CardData(
        val balanceEuros: Double,
        val rawBalanceUnits: Int,
        val cardType: CardType,
        val uid: String
    )

    enum class CardType { NORMAL, JOVEN, DESCONOCIDA }

    sealed class ReadResult {
        data class Success(val data: CardData) : ReadResult()
        data class Error(val message: String) : ReadResult()
    }

    /**
     * Conecta con la tarjeta, autentica los sectores necesarios y
     * devuelve el saldo y el tipo de tarjeta.
     */
    fun readCard(): ReadResult {
        val mifare = MifareClassic.get(tag)
            ?: return ReadResult.Error("Esta tarjeta no es Mifare Classic.")

        return try {
            mifare.connect()

            // --- Leer tipo de tarjeta (sector 8) ---
            if (!mifare.authenticateSectorWithKeyA(SECTOR_CONTRACT, KEY_A_SECTOR_8)) {
                return ReadResult.Error("No se pudo autenticar el sector 8 (datos de contrato).")
            }
            val contractBlockIndex = mifare.sectorToBlock(SECTOR_CONTRACT) + BLOCK_CONTRACT_INDEX_IN_SECTOR
            val contractBlock = mifare.readBlock(contractBlockIndex)
            val cardType = detectCardType(contractBlock)

            // --- Leer saldo (sector 9) ---
            if (!mifare.authenticateSectorWithKeyA(SECTOR_BALANCE, KEY_A_SECTOR_9)) {
                return ReadResult.Error("No se pudo autenticar el sector 9 (saldo).")
            }
            val balanceBlockIndex = mifare.sectorToBlock(SECTOR_BALANCE) + BLOCK_BALANCE_INDEX_IN_SECTOR
            val balanceBlock = mifare.readBlock(balanceBlockIndex)
            val rawUnits = parseValueBlock(balanceBlock)
            val balanceEuros = rawUnits * UNITS_TO_EURO

            val uid = tag.id.joinToString("") { "%02X".format(it) }

            ReadResult.Success(
                CardData(
                    balanceEuros = balanceEuros,
                    rawBalanceUnits = rawUnits,
                    cardType = cardType,
                    uid = uid
                )
            )
        } catch (e: Exception) {
            ReadResult.Error("Error leyendo la tarjeta: ${e.message}")
        } finally {
            try { mifare.close() } catch (_: Exception) {}
        }
    }

    /**
     * Escribe un nuevo saldo en el bloque 37 (sector 9). Requiere la Key B
     * del sector 9, que el usuario debe conocer; no viene incluida en la app.
     */
    fun writeBalance(newBalanceEuros: Double, keyBHex: String): ReadResult {
        val mifare = MifareClassic.get(tag)
            ?: return ReadResult.Error("Esta tarjeta no es Mifare Classic.")

        val keyB = try {
            hexStringToByteArray(keyBHex)
        } catch (e: Exception) {
            return ReadResult.Error("Key B inválida.")
        }

        return try {
            mifare.connect()

            if (!mifare.authenticateSectorWithKeyB(SECTOR_BALANCE, keyB)) {
                return ReadResult.Error("Key B incorrecta o no autorizada para escribir.")
            }

            val newUnits = Math.round(newBalanceEuros / UNITS_TO_EURO).toInt()
            val newBlock = buildValueBlock(newUnits, BLOCK_BALANCE_INDEX_IN_SECTOR)

            val balanceBlockIndex = mifare.sectorToBlock(SECTOR_BALANCE) + BLOCK_BALANCE_INDEX_IN_SECTOR
            mifare.writeBlock(balanceBlockIndex, newBlock)

            readCard()
        } catch (e: Exception) {
            ReadResult.Error("Error escribiendo en la tarjeta: ${e.message}")
        } finally {
            try { mifare.close() } catch (_: Exception) {}
        }
    }

    private fun parseValueBlock(block: ByteArray): Int {
        val buffer = ByteBuffer.wrap(block, 0, 4).order(ByteOrder.LITTLE_ENDIAN)
        return buffer.int
    }

    private fun buildValueBlock(value: Int, blockAddr: Int): ByteArray {
        val block = ByteArray(16)
        val buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value)
        val valueBytes = buffer.array()
        val invertedBytes = valueBytes.map { (it.toInt().inv() and 0xFF).toByte() }.toByteArray()

        // value, ~value, value
        System.arraycopy(valueBytes, 0, block, 0, 4)
        System.arraycopy(invertedBytes, 0, block, 4, 4)
        System.arraycopy(valueBytes, 0, block, 8, 4)

        // addr, ~addr, addr, ~addr
        val addr = blockAddr.toByte()
        val invAddr = (blockAddr.inv() and 0xFF).toByte()
        block[12] = addr
        block[13] = invAddr
        block[14] = addr
        block[15] = invAddr

        return block
    }

    /**
     * La identificación exacta del tipo (Normal / Joven) depende de bytes
     * concretos del bloque de contrato. El Consorcio no publica oficialmente
     * este formato, así que ajusta este criterio volcando tus propias tarjetas
     * y comparando los bytes que obtienes.
     */
    private fun detectCardType(contractBlock: ByteArray): CardType {
        val typeByte = contractBlock.getOrNull(0)?.toInt()?.and(0xFF) ?: return CardType.DESCONOCIDA
        return when (typeByte) {
            0x01 -> CardType.NORMAL
            0x02 -> CardType.JOVEN
            else -> CardType.DESCONOCIDA
        }
    }
}
