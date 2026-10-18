package top.likoslupus.cellulosesz.administration.worldtools

/** The fixed 5x5 TNT grid used by `/nuke`, in block offsets from the target column. */
internal object NukePattern {

    val offsets: List<Pair<Int, Int>> = buildList {
        for (x in -10..10 step 5) {
            for (z in -10..10 step 5) {
                add(x to z)
            }
        }
    }

}
