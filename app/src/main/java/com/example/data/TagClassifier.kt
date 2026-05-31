package com.example.data

object TagClassifier {
    private val artistTags = setOf(
        "yutaka_nakamura", "shingo_yamashita", "yoh_yoshinari", "hiroyuki_imaishi", 
        "norio_matsumoto", "mitsuo_isobo", "kou_yoshinari", "tatsuyuki_tanaka", 
        "umetsu_yasuomi", "masami_obari", "takashi_hashimoto", "sushi", "bahi_jd", 
        "hironori_tanaka", "norimoto_tokura", "toshiyuki_saito", "takeshi_koike", 
        "yoshimichi_kameda", "keiichiro_watanabe", "kenichi_kutsuna", "ryo_tachimori", 
        "weilin_zhang", "masahiro_ando", "hiromi_ishihama", "akira_amemiya", 
        "toshio_kawaguchi", "yoshihiko_umakoshi", "shingo_nonomura", "tatsurou_kawano", 
        "shinsaku_kozuma", "takahiro_shikama", "kenichi_yoshida", "sawada_shingo", 
        "atsushi_wakabayashi", "masahiko_otsuka", "hiroyuki_kitakubo", "kazuto_nakazawa", 
        "shinya_ohira", "shinji_hashimoto", "tetsuya_nishio", "shigeru_kimura", 
        "hiroshi_ikehata", "tadashi_hiramatsu", "yasunori_miyazawa", "masaaki_yuasa", 
        "kazuhiro_furuhashi", "megumi_kouno", "gosei_oda", "yuki_kamiya"
    )

    private val copyrightTags = setOf(
        "kekkai_sensen", "kekkai_sensen_series", "mob_psycho_100", "mob_psycho_100_series", 
        "one_punch_man", "one_punch_man_series", "flcl", "flcl_series", "naruto", "naruto_series", 
        "naruto_shippuuden", "evangelion", "tengen_toppa_gurren_lagann", "gurren_lagann", "fate", 
        "fate_series", "fate_grand_order", "fate_apocrypha", "fate_unlimited_blade_works", 
        "bleach", "bleach_series", "one_piece", "one_piece_series", "my_hero_academia", 
        "boku_no_hero_academia", "my_hero_academia_series", "jujutsu_kaisen", "jujutsu_kaisen_series", 
        "chainsaw_man", "demon_slayer", "kimetsu_no_yaiba", "sword_art_online", "kill_la_kill", 
        "attack_on_titan", "shingeki_no_kyojin", "madoka_magica", "mahou_shoujo_madoka_magica"
    )

    private val metadataTags = setOf(
        "sound", "music_video", "web_animation", "restoration", "commercial", 
        "promotional_video", "opening", "ending", "production_materials"
    )

    private val generalTags = setOf(
        "effects", "debris", "liquid", "smoke", "fire", "smears", "impact_frames", 
        "running", "morphing", "hair", "fabric", "fighting", "creatures", "animated", 
        "background_animation", "explosions", "beams", "lightning", "clouds", "wind", 
        "water", "ice", "dust", "sparkles", "laser", "mecha", "robot", "transformation", 
        "destruction", "camerawork", "slow_motion", "smear", "beams", "yutapon_cubes"
    )

    fun classify(tagName: String): SakugaTagCategory {
        val name = tagName.lowercase().trim()
        if (artistTags.contains(name)) return SakugaTagCategory.ARTIST
        if (copyrightTags.contains(name)) return SakugaTagCategory.COPYRIGHT
        if (metadataTags.contains(name)) return SakugaTagCategory.METADATA
        if (generalTags.contains(name)) return SakugaTagCategory.GENERAL

        if (name.endsWith("_series") || name.endsWith("_movie") || name.endsWith("_ova") || name.contains("_studioproduction")) {
            return SakugaTagCategory.COPYRIGHT
        }
        if (name.contains("acting") || name.contains("animation") || name.contains("effects") || name.contains("frames") || name.contains("smear") || name.contains("fighting")) {
            return SakugaTagCategory.GENERAL
        }

        // Heuristics for some structural patterns
        return SakugaTagCategory.GENERAL
    }

    fun extractCandidateName(text: String): String {
        var startIndex = 0
        while (startIndex < text.length) {
            val char = text[startIndex]
            if (char.isLetter()) {
                break
            }
            if (char.isDigit() || char == ':' || char == '.' || char == '-' || char == '–' || char == '~' || char.isWhitespace()) {
                startIndex++
            } else {
                startIndex++
            }
        }
        return text.substring(startIndex).trim()
    }

    fun isValidArtist(text: String, tagInfoMap: Map<String, SakugaTag> = emptyMap()): Boolean {
        val candidate = extractCandidateName(text)
        if (candidate.isEmpty()) return false

        // Step B Check Length First: Artist names are short.
        if (candidate.length > 20) {
            return false
        }

        // Step A: Artist Tag Database Cross-Referencing (Primary Method)
        val normalized = candidate.lowercase().replace(" ", "_").trim()
        
        // Check fetched/runtime Booru tag info map
        val tagFromMap = tagInfoMap[normalized]
        if (tagFromMap != null) {
            return tagFromMap.type == 1
        }
        
        // Check local database/heuristics classifier
        if (classify(normalized) == SakugaTagCategory.ARTIST) {
            return true
        }

        // Analyze string structure
        val words = candidate.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (words.size > 3) {
            return false
        }

        val isEntirelyLowercase = candidate == candidate.lowercase()
        val firstChar = candidate.firstOrNull()
        val startsWithLowercase = firstChar?.isLowerCase() == true
        if (isEntirelyLowercase || startsWithLowercase) {
            return false
        }

        // Step C: Conversational Word Filter
        val conversationalWords = setOf(
            "is", "are", "was", "were", "the", "a", "an", "and", "to", "amazing", "insane", 
            "good", "bad", "why", "teleport", "look", "think", "at", "energy", "of", "in", 
            "for", "on", "with", "by", "this", "that", "it", "so", "very", "much", "many", 
            "some", "any", "no", "yes", "or", "but", "about", "years", "ago", "hours", 
            "mins", "seconds", "secs", "frame", "frames", "clip", "clips", "animator", 
            "animation", "scene", "episode", "ep", "cut", "key", "gorgeous", "beautiful", 
            "incredible", "awesome", "cool", "great", "nice", "love", "like", "favorite", 
            "favourite", "has", "have", "had", "will", "would", "should", "could", "from"
        )
        if (words.any { conversationalWords.contains(it.lowercase()) }) {
            return false
        }

        return true
    }
}
