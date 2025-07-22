package com.tans.tapm.chipset

data class Chipset(
    val chipsetSeries: ChipsetSeries,
    val model: Int?,
    val suffix: String? = null
) {
    fun getChipsetName(): String {
        return if (chipsetSeries == ChipsetSeries.Unknown) {
            "Unknown"
        } else {
            val result = StringBuilder()
            result.append(chipsetSeries.vendor.vendorName)
            result.append(' ')
            result.append(chipsetSeries.seriesName)
            if (model != null) {
                result.append(model)
            }
            if (suffix != null) {
                result.append(suffix)
            }
            return result.toString()
        }
    }
}

data class SpecialMapEntry(
    val platform: String,
    val series: ChipsetSeries,
    val mode: Int? = null,
    val suffix: Char? = null
)

data class HuaweiMapEntry(
    val platform: String,
    val model: Int
)

private val tegraHardwareMapEntries by lazy {
    listOf(
        SpecialMapEntry(
            platform = "cardhu",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 30
        ),
        SpecialMapEntry(
            platform = "kai",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 30,
            suffix = 'L'
        ),
        SpecialMapEntry(
            platform = "p3",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 20
        ),
        SpecialMapEntry(
            platform = "n1",
            series = ChipsetSeries.NvidiaTegraAp,
            mode = 20,
            suffix = 'H'
        ),
        SpecialMapEntry(
            platform = "SHW-M380S",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 20
        ),
        SpecialMapEntry(
            platform = "m470",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 30,
            suffix = 'L'
        ),
        SpecialMapEntry(
            platform = "endeavoru",
            series = ChipsetSeries.NvidiaTegraAp,
            mode = 33
        ),
        SpecialMapEntry(
            platform = "enrc2b",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 33
        ),
        SpecialMapEntry(
            platform = "mozart",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 114
        ),
        SpecialMapEntry(
            platform = "tegratab",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 114
        ),
        SpecialMapEntry(
            platform = "tn8",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 124
        ),
        SpecialMapEntry(
            platform = "roth",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 114
        ),
        SpecialMapEntry(
            platform = "pisces",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 114
        ),
        SpecialMapEntry(
            platform = "mocha",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 124
        ),
        SpecialMapEntry(
            platform = "stingray",
            series = ChipsetSeries.NvidiaTegraAp,
            mode = 20,
            suffix = 'H'
        ),
        SpecialMapEntry(
            platform = "Ceres",
            series = ChipsetSeries.NvidiaTegraSl,
            mode = 460,
            suffix = 'N'
        ),
        SpecialMapEntry(
            platform = "MT799",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 30
        ),
        SpecialMapEntry(
            platform = "t8400n",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 114
        ),
        SpecialMapEntry(
            platform = "chagall",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 30
        ),
        SpecialMapEntry(
            platform = "ventana",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 20
        ),
        SpecialMapEntry(
            platform = "bobsleigh",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 33
        ),
        SpecialMapEntry(
            platform = "tegra_fjdev101",
            series = ChipsetSeries.NvidiaTegraAp,
            mode = 33
        ),
        SpecialMapEntry(
            platform = "tegra_fjdev103",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 33
        ),
        SpecialMapEntry(
            platform = "nbx03",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 20
        ),
        SpecialMapEntry(
            platform = "txs03",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 30,
            suffix = 'L'
        ),
        SpecialMapEntry(
            platform = "x3",
            series = ChipsetSeries.NvidiaTegraAp,
            mode = 33
        ),
        SpecialMapEntry(
            platform = "vu10",
            series = ChipsetSeries.NvidiaTegraAp,
            mode = 33
        ),
        SpecialMapEntry(
            platform = "BIRCH",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 30,
            suffix = 'L'
        ),
        SpecialMapEntry(
            platform = "macallan",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 114,
        ),
        SpecialMapEntry(
            platform = "maya",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 114,
        ),
        SpecialMapEntry(
            platform = "antares",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 20,
        ),
        SpecialMapEntry(
            platform = "tostab12AL",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 30,
            suffix = 'L'
        ),
        SpecialMapEntry(
            platform = "tostab12BL",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 30,
            suffix = 'L'
        ),
        SpecialMapEntry(
            platform = "sphinx",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 30,
        ),
        SpecialMapEntry(
            platform = "tostab11BS",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 30,
        ),
        SpecialMapEntry(
            platform = "tostab12BA",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 114,
        ),
        SpecialMapEntry(
            platform = "vangogh",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 20,
        ),
        SpecialMapEntry(
            platform = "a110",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 30,
            suffix = 'L'
        ),
        SpecialMapEntry(
            platform = "picasso_e",
            series = ChipsetSeries.NvidiaTegraAp,
            mode = 20,
            suffix = 'H'
        ),

        SpecialMapEntry(
            platform = "picasso_e2",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 30,
            suffix = 'L'
        ),
        SpecialMapEntry(
            platform = "picasso",
            series = ChipsetSeries.NvidiaTegraAp,
            mode = 20,
            suffix = 'H'
        ),
        SpecialMapEntry(
            platform = "picasso_m",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 30,
        ),
        SpecialMapEntry(
            platform = "picasso_mf",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 30,
        ),
        SpecialMapEntry(
            platform = "avalon",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 30,
            suffix = 'L'
        ),
        SpecialMapEntry(
            platform = "NS_14T004",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 30,
            suffix = 'L'
        ),
        SpecialMapEntry(
            platform = "WIKIPAD",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 30,
        ),
        SpecialMapEntry(
            platform = "kb",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 114,
        ),
        SpecialMapEntry(
            platform = "foster_e",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 210,
        ),
        SpecialMapEntry(
            platform = "foster_e_hdd",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 210,
        ),
        SpecialMapEntry(
            platform = "darcy",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 210,
        ),
    )
}

private val specialMapEntries by lazy {
    listOf(
        SpecialMapEntry(
            platform = "k3v2oem1",
            series = ChipsetSeries.HisiliconK3v,
            mode = 2
        ),
        SpecialMapEntry(
            platform = "hi6620oem",
            series = ChipsetSeries.HisiliconKirin,
            mode = 910,
            suffix = 'T'
        ),
        SpecialMapEntry(
            platform = "hi6250",
            series = ChipsetSeries.HisiliconKirin,
            mode = 650
        ),
        SpecialMapEntry(
            platform = "hi6210sft",
            series = ChipsetSeries.HisiliconKirin,
            mode = 620
        ),
        SpecialMapEntry(
            platform = "hi3751",
            series = ChipsetSeries.HisiliconHi,
            mode = 3751
        ),

        SpecialMapEntry(
            platform = "hi3630",
            series = ChipsetSeries.HisiliconKirin,
            mode = 920
        ),

        SpecialMapEntry(
            platform = "hi3635",
            series = ChipsetSeries.HisiliconKirin,
            mode = 930
        ),

        SpecialMapEntry(
            platform = "gs702a",
            series = ChipsetSeries.ActionsAtm,
            mode = 7029
        ),

        SpecialMapEntry(
            platform = "gs702c",
            series = ChipsetSeries.ActionsAtm,
            mode = 7029,
            suffix = 'B'
        ),

        SpecialMapEntry(
            platform = "gs703d",
            series = ChipsetSeries.ActionsAtm,
            mode = 7039,
            suffix = 'S'
        ),

        SpecialMapEntry(
            platform = "gs705a",
            series = ChipsetSeries.ActionsAtm,
            mode = 7059,
            suffix = 'A'
        ),

        SpecialMapEntry(
            platform = "Amlogic Meson8",
            series = ChipsetSeries.AmlogicS,
            mode = 812
        ),
        SpecialMapEntry(
            platform = "Amlogic Meson8B",
            series = ChipsetSeries.AmlogicS,
            mode = 805
        ),
        SpecialMapEntry(
            platform = "mapphone_CDMA",
            series = ChipsetSeries.TexasInstrumentsOmap,
            mode = 4430
        ),

        SpecialMapEntry(
            platform = "Superior",
            series = ChipsetSeries.TexasInstrumentsOmap,
            mode = 4470
        ),
        SpecialMapEntry(
            platform = "Tuna",
            series = ChipsetSeries.TexasInstrumentsOmap,
            mode = 4460
        ),
        SpecialMapEntry(
            platform = "Manta",
            series = ChipsetSeries.SamsungExynos,
            mode = 5250
        ),
        SpecialMapEntry(
            platform = "Odin",
            series = ChipsetSeries.LGNuclun,
            mode = 7111
        ),
        SpecialMapEntry(
            platform = "Madison",
            series = ChipsetSeries.MStar6a,
            mode = 338
        ),
    )
}

private val huaweiMapEntries by lazy {
    listOf(
        HuaweiMapEntry(
            platform = "ALP",
            model = 970
        ),
        HuaweiMapEntry(
            platform = "BAC",
            model = 659
        ),
        HuaweiMapEntry(
            platform = "BLA",
            model = 970
        ),
        HuaweiMapEntry(
            platform = "BKL",
            model = 970
        ),
        HuaweiMapEntry(
            platform = "CLT",
            model = 970
        ),
        HuaweiMapEntry(
            platform = "COL",
            model = 970
        ),
        HuaweiMapEntry(
            platform = "COR",
            model = 970
        ),
        HuaweiMapEntry(
            platform = "DUK",
            model = 960
        ),
        HuaweiMapEntry(
            platform = "EML",
            model = 970
        ),
        HuaweiMapEntry(
            platform = "EVA",
            model = 955
        ),
        HuaweiMapEntry(
            platform = "FRD",
            model = 950
        ),
        HuaweiMapEntry(
            platform = "INE",
            model = 710
        ),
        HuaweiMapEntry(
            platform = "KNT",
            model = 950
        ),
        HuaweiMapEntry(
            platform = "LON",
            model = 960
        ),
        HuaweiMapEntry(
            platform = "LYA",
            model = 980
        ),
        HuaweiMapEntry(
            platform = "MCN",
            model = 980
        ),
        HuaweiMapEntry(
            platform = "MHA",
            model = 960
        ),
        HuaweiMapEntry(
            platform = "NEO",
            model = 970
        ),
        HuaweiMapEntry(
            platform = "NXT",
            model = 950
        ),
        HuaweiMapEntry(
            platform = "PAN",
            model = 980
        ),
        HuaweiMapEntry(
            platform = "PAR",
            model = 970
        ),
        HuaweiMapEntry(
            platform = "RVL",
            model = 970
        ),
        HuaweiMapEntry(
            platform = "STF",
            model = 960
        ),
        HuaweiMapEntry(
            platform = "SUE",
            model = 980
        ),

        HuaweiMapEntry(
            platform = "VKY",
            model = 960
        ),
        HuaweiMapEntry(
            platform = "VTR",
            model = 960
        ),

        )
}

private val specialBoardMapEntries by lazy {
    listOf(
        SpecialMapEntry(
            platform = "hi6250",
            series = ChipsetSeries.HisiliconKirin,
            mode = 650
        ),
        SpecialMapEntry(
            platform = "hi6210sft",
            series = ChipsetSeries.HisiliconKirin,
            mode = 620
        ),
        SpecialMapEntry(
            platform = "hi3630",
            series = ChipsetSeries.HisiliconKirin,
            mode = 920
        ),
        SpecialMapEntry(
            platform = "hi3635",
            series = ChipsetSeries.HisiliconKirin,
            mode = 930
        ),
        SpecialMapEntry(
            platform = "hi3650",
            series = ChipsetSeries.HisiliconKirin,
            mode = 950
        ),
        SpecialMapEntry(
            platform = "hi3660",
            series = ChipsetSeries.HisiliconKirin,
            mode = 960
        ),
        SpecialMapEntry(
            platform = "mp523x",
            series = ChipsetSeries.RenesasMp,
            mode = 5232
        ),
        SpecialMapEntry(
            platform = "BEETHOVEN",
            series = ChipsetSeries.HisiliconKirin,
            mode = 950
        ),
        SpecialMapEntry(
            platform = "hws7701u",
            series = ChipsetSeries.RockchipRk,
            mode = 3168
        ),
        SpecialMapEntry(
            platform = "g2mv",
            series = ChipsetSeries.NvidiaTegraSl,
            mode = 460,
            suffix = 'N',
        ),
        SpecialMapEntry(
            platform = "K00F",
            series = ChipsetSeries.RockchipRk,
            mode = 3188
        ),
        SpecialMapEntry(
            platform = "T7H",
            series = ChipsetSeries.RockchipRk,
            mode = 3066
        ),
        SpecialMapEntry(
            platform = "tuna",
            series = ChipsetSeries.TexasInstrumentsOmap,
            mode = 4460
        ),
        SpecialMapEntry(
            platform = "grouper",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 30,
            suffix = 'L'
        ),
        SpecialMapEntry(
            platform = "flounder",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 132,
        ),
        SpecialMapEntry(
            platform = "dragon",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 210,
        ),

        SpecialMapEntry(
            platform = "sailfish",
            series = ChipsetSeries.QualcommMsm,
            mode = 8996,
            suffix = 'P'
        ),
        SpecialMapEntry(
            platform = "marlin",
            series = ChipsetSeries.QualcommMsm,
            mode = 8996,
            suffix = 'P'
        ),
    )
}

private val specialPlatformMapEntries by lazy {
    listOf(
        SpecialMapEntry(
            platform = "hi6620oem",
            series = ChipsetSeries.HisiliconKirin,
            mode = 910,
            suffix = 'T'
        ),
        SpecialMapEntry(
            platform = "hi6250",
            series = ChipsetSeries.HisiliconKirin,
            mode = 650,
        ),
        SpecialMapEntry(
            platform = "hi6210sft",
            series = ChipsetSeries.HisiliconKirin,
            mode = 620,
        ),
        SpecialMapEntry(
            platform = "hi3630",
            series = ChipsetSeries.HisiliconKirin,
            mode = 920,
        ),
        SpecialMapEntry(
            platform = "hi3635",
            series = ChipsetSeries.HisiliconKirin,
            mode = 930,
        ),
        SpecialMapEntry(
            platform = "hi3650",
            series = ChipsetSeries.HisiliconKirin,
            mode = 950,
        ),
        SpecialMapEntry(
            platform = "hi3660",
            series = ChipsetSeries.HisiliconKirin,
            mode = 960,
        ),
        SpecialMapEntry(
            platform = "k3v200",
            series = ChipsetSeries.HisiliconK3v,
            mode = 2
        ),
        SpecialMapEntry(
            platform = "montblanc",
            series = ChipsetSeries.NovathorU,
            mode = 8500,
        ),
        SpecialMapEntry(
            platform = "song",
            series = ChipsetSeries.PineconeSurgeS,
            mode = 1
        ),
        SpecialMapEntry(
            platform = "rk322x",
            series = ChipsetSeries.RockchipRk,
            mode = 3229
        ),
        SpecialMapEntry(
            platform = "tegra132",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 132
        ),
        SpecialMapEntry(
            platform = "tegra210_dragon",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 210
        ),
        SpecialMapEntry(
            platform = "tegra4",
            series = ChipsetSeries.NvidiaTegraT,
            mode = 114
        ),
        SpecialMapEntry(
            platform = "s5pc110",
            series = ChipsetSeries.SamsungExynos,
            mode = 3110
        ),
    )
}

fun getChipsetInfo(): Chipset {
    val roBoardPlatform = readSystemProperty("ro.board.platform")
    val cpuInfo = readCpuInfo()
    val isTegraPlatform = roBoardPlatform == "tegra" || roBoardPlatform == "tegra3"
    val cpuInfoHardware: String? = cpuInfo["Hardware"]?.getOrNull(0)?.trim()
    var result = decodeChipsetFromProcCpuinfoHardware(cpuInfoHardware, isTegraPlatform)
    if (result.chipsetSeries != ChipsetSeries.Unknown) {
        return result
    }
    val roProductBoard = readSystemProperty("ro.product.board")
    result = decodeChipsetFromRoProductBoard(roProductBoard)
    if (result.chipsetSeries != ChipsetSeries.Unknown) {
        return result
    }
    result = decodeChipsetFromRoBoardPlatform(roBoardPlatform)
    if (result.chipsetSeries != ChipsetSeries.Unknown) {
        return result
    }
    val roMediatekPlatform = readSystemProperty("ro.mediatek.platform")
    result = decodeChipsetFromRoMediatekPlatform(roMediatekPlatform)
    if (result.chipsetSeries != ChipsetSeries.Unknown) {
        return result
    }
    val roArch = readSystemProperty("ro.arch")
    result = decodeChipsetFromRoArch(roArch)
    if (result.chipsetSeries != ChipsetSeries.Unknown) {
        return result
    }
    val roChipname = readSystemProperty("ro.chipname")
    result = decodeChipsetFromRoChipname(roChipname)
    if (result.chipsetSeries != ChipsetSeries.Unknown) {
        return result
    }
    val roHardwareChipname = readSystemProperty("ro.hardware.chipname")
    result = decodeChipsetFromRoChipname(roHardwareChipname)
    if (result.chipsetSeries != ChipsetSeries.Unknown) {
        return result
    }
    val roSocModel = readSystemProperty("ro.soc.model")
    return decodeChipsetFromProcCpuinfoHardware(roSocModel, isTegraPlatform)
}

/**
 * msm8953pro
 */
private fun tryDecodeMsmOrApq(hardware: String): Chipset? {
    if (hardware.length < 7) {
        return null
    }
    val hardwareLowercase = hardware.lowercase()
    val regex = ".*(msm|apq)([0-9]{4})(.*)".toRegex()
    val matchResult = regex.find(hardwareLowercase)
    if (matchResult == null) {
        return null
    }
    val values = matchResult.groupValues
    val model = values[2].toInt()
    val suffix = values[3]
    return Chipset(
        chipsetSeries = if (values[1] == "msm") ChipsetSeries.QualcommMsm else ChipsetSeries.QualcommApq,
        model = model,
        suffix = suffix.ifEmpty { null }
    )
}

/**
 * sdm888
 */
private fun tryDecodeSdm(hardware: String): Chipset? {
    if (hardware.length != 6) {
        return null
    }
    val hardwareLowercase = hardware.lowercase()
    val regex = "sdm([0-9]{3})".toRegex()
    val matchResult = regex.find(hardwareLowercase)
    if (matchResult == null) {
        return null
    }

    return Chipset(
        chipsetSeries = ChipsetSeries.QualcommSnapdragon,
        model = matchResult.groupValues[1].toInt()
    )
}

/**
 * sm8550
 */
private fun tryDecodeSm(hardware: String): Chipset? {
    if (hardware.length != 6) {
        return null
    }
    val hardwareLowercase = hardware.lowercase()
    val regex = "sm([0-9]{4})".toRegex()
    val matchResult = regex.find(hardwareLowercase)
    if (matchResult == null) {
        return null
    }
    return Chipset(
        chipsetSeries = ChipsetSeries.QualcommSnapdragon,
        model = matchResult.groupValues[1].toInt()
    )
}

/**
 * MT6833V/PNZA
 */
private fun tryDecodeMt(hardware: String): Chipset? {
    if (hardware.length < 6) {
        return null
    }
    val hardwareLowercase = hardware.lowercase()
    val regex = "(mt|mtk)([0-9]{4}).*".toRegex()
    val matchResult = regex.find(hardwareLowercase)
    if (matchResult == null) {
        return null
    }
    val suffixRegex = ".*[0-9]{4}(.*)".toRegex()
    val suffix = suffixRegex.find(hardware)?.groupValues?.get(1)
    return Chipset(
        chipsetSeries = ChipsetSeries.MediaTekMt,
        model = matchResult.groupValues[2].toInt(),
        suffix = if (suffix.isNullOrBlank()) null else suffix
    )
}

/**
 * Kirin 980
 */
private fun tryDecodeKirin(hardware: String): Chipset? {
    val regex = "^[K|k]irin ?([0-9]{3})".toRegex()
    val matchResult = regex.find(hardware)
    if (matchResult == null) {
        return null
    }
    return Chipset(
        chipsetSeries = ChipsetSeries.HisiliconKirin,
        model = matchResult.groupValues[1].toInt()
    )
}

/**
 * Expect 6-7 symbols: "RK" (2 symbols) + 4-digit model number + optional 1-letter suffix
 * RK1000T
 *
 */
private fun tryDecodeRock(hardware: String): Chipset? {
    val regex = "RK([0-9]{4})(.?)".toRegex()
    val matchResult = regex.find(hardware)
    if (matchResult == null) {
        return null
    }
    return Chipset(
        chipsetSeries = ChipsetSeries.RockchipRk,
        model = matchResult.groupValues[1].toInt(),
        suffix = matchResult.groupValues[2].ifBlank { null }
    )
}

/**
 * 	Expect at 18-19 symbols:
 * 	 - "Samsung" (7 symbols) + space + "Exynos" (6 symbols) + optional space + 4-digit model number
 */
private fun tryDecodeSamsungExynos(hardware: String): Chipset? {
    val regex = "Samsung Exynos ?([0-9]{4})".toRegex()
    val matchResult = regex.find(hardware)
    if (matchResult == null) {
        return null
    }
    return Chipset(
        chipsetSeries = ChipsetSeries.SamsungExynos,
        model = matchResult.groupValues[1].toInt(),
    )
}

/**
 * Expect exactly 13 symbols: "universal" (9 symbols) + 4-digit model
 * number
 */
private fun tryDecodeUniversal(hardware: String): Chipset? {
    val regex = "universal([0-9]{4})".toRegex()
    val matchResult = regex.find(hardware)
    if (matchResult == null) {
        return null
    }
    return Chipset(
        chipsetSeries = ChipsetSeries.SamsungExynos,
        model = matchResult.groupValues[1].toInt(),
    )
}

/**
 * Expect exactly 8 symbols: "SMDK" (4 symbols) + 4-digit model number
 */
private fun tryDecodeSmdk(hardware: String): Chipset? {
    val regex = "SMDK([0-9]{4})".toRegex()
    val matchResult = regex.find(hardware)
    if (matchResult == null) {
        return null
    }
    return Chipset(
        chipsetSeries = ChipsetSeries.SamsungExynos,
        model = matchResult.groupValues[1].toInt(),
    )
}

private fun tryDecodeS5e(hardware: String): Chipset? {
    val regex = "s5e([0-9]{4})".toRegex()
    val matchResult = regex.find(hardware)
    if (matchResult == null) {
        return null
    }
    return Chipset(
        chipsetSeries = ChipsetSeries.SamsungExynos,
        model = matchResult.groupValues[1].toInt(),
    )
}

private fun tryDecodeTensor(hardware: String): Chipset? {
    if (hardware == "Tensor") {
        return Chipset(
            chipsetSeries = ChipsetSeries.GoogleTensor,
            model = 1,
        )
    } else {
        val regex = "Tensor G([0-9]+)".toRegex()
        val matchResult = regex.find(hardware)
        if (matchResult == null) {
            return null
        }
        return Chipset(
            chipsetSeries = ChipsetSeries.GoogleTensor,
            model = matchResult.groupValues[1].toInt(),
        )
    }
}

private fun tryDecodeSc(hardware: String): Chipset? {
    if (!hardware.startsWith("sc") && !hardware.startsWith("sp")) {
        return null
    }
    val index2Char = hardware.getOrNull(2)
    if (index2Char != null) {
        if (index2Char == 'x') {
            return if (hardware.endsWith("15")) {
                Chipset(
                    chipsetSeries = ChipsetSeries.SpreadtrumSc,
                    model = 7715
                )
            } else {
                null
            }
        } else {
            val regex = "s[cp]([0-9]{4})(.+)".toRegex()
            val matchResult = regex.find(hardware)
            if (matchResult == null) {
                return null
            }
            return Chipset(
                chipsetSeries = ChipsetSeries.SpreadtrumSc,
                model = matchResult.groupValues[1].toInt(),
                suffix = if (matchResult.groupValues[2].isBlank()) null else matchResult.groupValues[2].uppercase()
            )
        }
    } else {
        return null
    }
}

private fun tryDecodeT(hardware: String): Chipset? {
    val regex = "Unisoc T([0-9]+)".toRegex()
    val matchResult = regex.find(hardware)
    if (matchResult == null) {
        return null
    }
    return Chipset(
        chipsetSeries = ChipsetSeries.UnisocT,
        model = matchResult.groupValues[1].toInt(),
    )
}

private fun tryDecodeUms(hardware: String): Chipset? {
    val regex = "Unisoc UMS([0-9]+)".toRegex()
    val matchResult = regex.find(hardware)
    if (matchResult == null) {
        return null
    }
    return Chipset(
        chipsetSeries = ChipsetSeries.UnisocUms,
        model = matchResult.groupValues[1].toInt(),
    )
}

private fun tryDecodeLc(hardware: String): Chipset? {
    val regex = "lc([0-9]{4})".toRegex()
    val matchResult = regex.find(hardware)
    if (matchResult == null) {
        return null
    }
    return Chipset(
        chipsetSeries = ChipsetSeries.LeadcoreLc,
        model = matchResult.groupValues[1].toInt()
    )
}

private fun tryDecodePxa(hardware: String): Chipset? {
    val regex = "PXA([0-9]+)".toRegex()
    val matchResult = regex.find(hardware)
    if (matchResult == null) {
        return null
    }
    return Chipset(
        chipsetSeries = ChipsetSeries.MarvellPxa,
        model = matchResult.groupValues[1].toInt()
    )
}

private fun tryDecodeBcm(hardware: String): Chipset? {
    val regex = "BCM([0-9]{4})".toRegex()
    val matchResult = regex.find(hardware)
    if (matchResult == null) {
        return null
    }
    return Chipset(
        chipsetSeries = ChipsetSeries.BroadcomBcm,
        model = matchResult.groupValues[1].toInt()
    )
}

private fun tryDecodeOmap(hardware: String): Chipset? {
    val regex = "OMAP([0-9]{4})".toRegex()
    val matchResult = regex.find(hardware)
    if (matchResult == null) {
        return null
    }
    return Chipset(
        chipsetSeries = ChipsetSeries.TexasInstrumentsOmap,
        model = matchResult.groupValues[1].toInt(),
    )
}

private fun tryDeocdeTcc(hardware: String): Chipset? {
    val regex = "tcc([0-9]{3})x".toRegex()
    val matchResult = regex.find(hardware)
    if (matchResult == null) {
        return null
    }
    return Chipset(
        chipsetSeries = ChipsetSeries.TelechipsTcc,
        model = matchResult.groupValues[1].toInt(),
        suffix = "X"
    )
}

/**
 * Expect length of either 3, 7 or 8, exactly:
 * - 3-letter platform identifier (see huawei_platform_map)
 * - 3-letter platform identifier + '-' + 'L' + two digits
 * - 3-letter platform identifier + '-' + capital letter + 'L' + two
 * digits
 */
private fun tryDecodeHuawei(hardware: String): Chipset? {
    val len = hardware.length
    if (len != 3 && len != 7 && len != 8) {
        return null
    }
    val platform = hardware.take(3)
    for (entry in huaweiMapEntries) {
        if (entry.platform == platform) {
            return Chipset(
                chipsetSeries = ChipsetSeries.HisiliconKirin,
                model = entry.model
            )
        }
    }
    return null
}

/**
 * Expect exactly 10 symbols: "exynos" (6 symbols) + 4-digit model number
 */
private fun tryDecodeExynos(hardware: String): Chipset? {
    if (hardware.length != 10) {
        return null
    }
    val regex = "exynos([0-9]{4})".toRegex()
    val matchResult = regex.find(hardware)
    if (matchResult == null) {
        return null
    }
    return Chipset(
        chipsetSeries = ChipsetSeries.SamsungExynos,
        model = matchResult.groupValues[1].toInt(),
    )
}

private fun decodeChipsetFromProcCpuinfoHardware(
    hardware: String?,
    isTegraPlatform: Boolean
): Chipset {
    if (hardware != null) {
        if (isTegraPlatform) {
            for (entry in tegraHardwareMapEntries) {
                if (entry.platform == hardware) {
                    return Chipset(
                        chipsetSeries = entry.series,
                        model = entry.mode,
                        suffix = entry.suffix?.toString()
                    )
                }
            }
        } else {
            var result: Chipset? = tryDecodeMsmOrApq(hardware)
            if (result != null) {
                return result
            }
            result = tryDecodeSdm(hardware)
            if (result != null) {
                return result
            }
            result = tryDecodeSm(hardware)
            if (result != null) {
                return result
            }
            result = tryDecodeMt(hardware)
            if (result != null) {
                return result
            }
            result = tryDecodeKirin(hardware)
            if (result != null) {
                return result
            }
            result = tryDecodeRock(hardware)
            if (result != null) {
                return result
            }
            result = tryDecodeSamsungExynos(hardware)
            if (result != null) {
                return result
            }
            result = tryDecodeUniversal(hardware)
            if (result != null) {
                return result
            }
            result = tryDecodeSmdk(hardware)
            if (result != null) {
                return result
            }
            result = tryDecodeS5e(hardware)
            if (result != null) {
                return result
            }
            result = tryDecodeTensor(hardware)
            if (result != null) {
                return result
            }
            result = tryDecodeSc(hardware)
            if (result != null) {
                return result
            }
            result = tryDecodeT(hardware)
            if (result != null) {
                return result
            }
            result = tryDecodeUms(hardware)
            if (result != null) {
                return result
            }
            result = tryDecodePxa(hardware)
            if (result != null) {
                return result
            }
            result = tryDecodeBcm(hardware)
            if (result != null) {
                return result
            }
            result = tryDecodeOmap(hardware)
            if (result != null) {
                return result
            }
            result = tryDeocdeTcc(hardware)
            if (result != null) {
                return result
            }
            for (entry in specialMapEntries) {
                if (entry.platform == hardware) {
                    return Chipset(
                        chipsetSeries = entry.series,
                        model = entry.mode,
                        suffix = entry.suffix?.toString(),
                    )
                }
            }
        }
    }
    return Chipset(
        chipsetSeries = ChipsetSeries.Unknown,
        model = null,
    )
}

private fun decodeChipsetFromRoProductBoard(
    roProductBoard: String?
): Chipset {
    if (roProductBoard != null) {
        var result: Chipset? = tryDecodeMsmOrApq(roProductBoard)
        if (result != null) {
            return result
        }
        result = tryDecodeUniversal(roProductBoard)
        if (result != null) {
            return result
        }

        result = tryDecodeSmdk(roProductBoard)
        if (result != null) {
            return result
        }

        result = tryDecodeMt(roProductBoard)
        if (result != null) {
            return result
        }

        result = tryDecodeSc(roProductBoard)
        if (result != null) {
            return result
        }

        result = tryDecodePxa(roProductBoard)
        if (result != null) {
            return result
        }

        result = tryDecodeLc(roProductBoard)
        if (result != null) {
            return result
        }

        result = tryDecodeBcm(roProductBoard)
        if (result != null) {
            return result
        }

        result = tryDecodeHuawei(roProductBoard)
        if (result != null) {
            return result
        }

        for (entry in specialBoardMapEntries) {
            if (entry.platform == roProductBoard) {
                return Chipset(
                    chipsetSeries = entry.series,
                    model = entry.mode,
                    suffix = entry.suffix?.toString(),
                )
            }
        }
    }
    return Chipset(
        chipsetSeries = ChipsetSeries.Unknown,
        model = null,
    )
}

private fun decodeChipsetFromRoBoardPlatform(
    roBoardPlatform: String?
): Chipset {
    if (roBoardPlatform != null) {
        var result = tryDecodeMsmOrApq(roBoardPlatform)
        if (result != null) {
            return result
        }
        result = tryDecodeExynos(roBoardPlatform)
        if (result != null) {
            return result
        }
        result = tryDecodeMt(roBoardPlatform)
        if (result != null) {
            return result
        }
        result = tryDecodeKirin(roBoardPlatform)
        if (result != null) {
            return result
        }
        result = tryDecodeSc(roBoardPlatform)
        if (result != null) {
            return result
        }
        result = tryDecodeRock(roBoardPlatform)
        if (result != null) {
            return result
        }
        result = tryDecodeLc(roBoardPlatform)
        if (result != null) {
            return result
        }
        result = tryDecodeHuawei(roBoardPlatform)
        if (result != null) {
            return result
        }
        result = tryDecodeBcm(roBoardPlatform)
        if (result != null) {
            return result
        }
        for (entry in specialPlatformMapEntries) {
            if (entry.platform == roBoardPlatform) {
                return Chipset(
                    chipsetSeries = entry.series,
                    model = entry.mode,
                    suffix = entry.suffix?.toString()
                )
            }
        }
    }
    return Chipset(
        chipsetSeries = ChipsetSeries.Unknown,
        model = null,
    )
}

private fun decodeChipsetFromRoMediatekPlatform(
    roMediatekPlatform: String?
): Chipset {
    if (roMediatekPlatform != null) {
        val result = tryDecodeMt(roMediatekPlatform)
        if (result != null) {
            return result
        }
    }
    return Chipset(
        chipsetSeries = ChipsetSeries.Unknown,
        model = null
    )
}

private fun decodeChipsetFromRoArch(
    roArch: String?
): Chipset {
    if (roArch != null) {
        val result = tryDecodeExynos(roArch)
        if (result != null) {
            return result
        }
    }
    return Chipset(
        chipsetSeries = ChipsetSeries.Unknown,
        model = null,
    )
}

private fun decodeChipsetFromRoChipname(
    roChipname: String?,
): Chipset {
    if (roChipname != null) {
        var result = tryDecodeMsmOrApq(roChipname)
        if (result != null) {
            return result
        }
        result = tryDecodeSm(roChipname)
        if (result != null) {
            return result
        }
        result = tryDecodeExynos(roChipname)
        if (result != null) {
            return result
        }
        result = tryDecodeUniversal(roChipname)
        if (result != null) {
            return result
        }
        result = tryDecodeMt(roChipname)
        if (result != null) {
            return result
        }
        result = tryDecodeSc(roChipname)
        if (result != null) {
            return result
        }
        result = tryDecodePxa(roChipname)
        if (result != null) {
            return result
        }
    }
    return Chipset(
        chipsetSeries = ChipsetSeries.Unknown,
        model = null
    )
}