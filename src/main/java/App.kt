import java.io.File
import java.io.OutputStreamWriter

data class Photo(
        val id: Int,
        val orientation: Orientation,
        val tags: Set<String>
)

data class Slide(
        val photoIds: Set<Int>,
        val tags: Set<String>
)

enum class Orientation {
    HORIZONTAL,
    VERTICAL
}

enum class Files(val filename: String) {
    EXAMPLE_A("a_example"),
    EXAMPLE_B("b_lovely_landscapes"),
    EXAMPLE_C("c_memorable_moments"),
    EXAMPLE_D("d_pet_pictures"),
    EXAMPLE_E("e_shiny_selfies")
}

fun main(args: Array<String>) {
    val filenames = Files.values().map { it.filename }
    filenames.forEach { filename ->
        Thread().run {
            processFile(filename)
        }
    }

}

fun processFile(filename: String) {

    val file = File("src/$filename.txt")
    val photos = file
            .readLines()
            .filterIndexed { index, _ -> index != 0 }
            .mapIndexed { index, value ->
                val splittedItems = value.split(" ")
                val orientation = if (splittedItems[0] == "H") {
                    Orientation.HORIZONTAL
                } else {
                    Orientation.VERTICAL
                }
                val tags = splittedItems.subList(2, splittedItems.size)

                Photo(
                        id = index,
                        orientation = orientation,
                        tags = tags.toSet())
            }

    val horizontalPhotos = photos.filter { it.orientation == Orientation.HORIZONTAL }
    val verticalPhotos = photos.filter { it.orientation == Orientation.VERTICAL }

    val horizontalSlides = horizontalPhotos.map { it.toSlide() }

    val verticalSlides = mutableListOf<Slide>()
    val mutableVerticalPhotos = verticalPhotos.toMutableList()
    slidingWindowCalculation(mutableVerticalPhotos, verticalSlides)

    val resultVerticals = mutableListOf<Slide>()
    val resultHorizontals = mutableListOf<Slide>()
    val horizontalMutableSlides = horizontalSlides.toMutableList()
    val mutableVerticalSlides = verticalSlides.toMutableList()

    slidingWindowCalculationForSlides(mutableVerticalSlides, resultVerticals)
    slidingWindowCalculationForSlides(horizontalMutableSlides, resultHorizontals)

    val result = resultHorizontals + resultVerticals

    val writer = File("${filename}_result.txt").writer()
    writer.writeLine(result.size.toString())
    result.forEach { writer.writeLine(it.toLine()) }
    writer.close()
}


private fun slidingWindowCalculation(
        currentMutableList: MutableList<Photo>,
        resultVerticals: MutableList<Slide>
) {

    var window = 20001
    while (currentMutableList.isNotEmpty()) {
        when {
            currentMutableList.size == 1 -> {
                resultVerticals.add(currentMutableList[0].toSlide())
                currentMutableList.remove(currentMutableList[0])
            }

            currentMutableList.size == 2 -> {
                resultVerticals.add(currentMutableList.toSlide())
                currentMutableList.remove(currentMutableList[0])
                currentMutableList.remove(currentMutableList[0])
            }

            currentMutableList.size < window -> {
                window -= 2
            }

            else -> {
                val centerIndex = window / 2
                val centerElement = currentMutableList[centerIndex]
                val leftSublist = currentMutableList.subList(0, centerIndex)
                val rightSublist = currentMutableList.subList(centerIndex + 1, window)

                val allWithoutCenterSublist = leftSublist + rightSublist

                val bestMatch = allWithoutCenterSublist.maxBy { calculateScore(centerElement, it) }!!
                resultVerticals.add(Pair(centerElement, bestMatch).toSlide())

                currentMutableList.remove(centerElement)
                currentMutableList.remove(bestMatch)
            }
        }
    }
}

private fun slidingWindowCalculationForSlides(
        currentMutableList: MutableList<Slide>,
        resultVerticals: MutableList<Slide>
) {

    var window = 20001

    while (currentMutableList.isNotEmpty()) {
        when {
            currentMutableList.size == 1 -> {
                resultVerticals.add(currentMutableList[0])
                currentMutableList.remove(currentMutableList[0])
            }

            currentMutableList.size == 2 -> {
                resultVerticals.addAll(currentMutableList)
                currentMutableList.remove(currentMutableList[0])
                currentMutableList.remove(currentMutableList[0])
            }

            currentMutableList.size < window -> {
                window -= 2
            }

            else -> {
                val centerIndex = window / 2
                val centerElement = currentMutableList[centerIndex]
                val leftSublist = currentMutableList.subList(0, centerIndex)
                val rightSublist = currentMutableList.subList(centerIndex + 1, window)

                val allWithoutCenterSublist = leftSublist + rightSublist

                val bestMatch = allWithoutCenterSublist.maxBy { calculateScore(centerElement, it) }!!
                resultVerticals.addAll(listOf(centerElement, bestMatch))

                currentMutableList.remove(centerElement)
                currentMutableList.remove(bestMatch)
            }
        }
    }
}

fun Photo.toSlide(): Slide = Slide(
        photoIds = setOf(this.id),
        tags = this.tags
)

fun Pair<Photo, Photo>.toSlide(): Slide = Slide(
        photoIds = setOf(this.first.id, this.second.id),
        tags = this.first.tags + this.second.tags
)

fun List<Photo>.toSlide(): Slide = Slide(
        photoIds = setOf(get(0).id, get(1).id),
        tags = get(0).tags + get(1).tags
)

fun Slide.toLine(): String = this.photoIds.joinToString(separator = " ")

fun OutputStreamWriter.writeLine(line: String) {
    this.write(line)
    this.write("\n")
}

fun calculateScore(photoA: Photo, photoB: Photo): Int {
    return (photoA.tags + photoB.tags).size
}

fun calculateScore(slideA: Slide, slideB: Slide): Int {
    val unionScore = slideA.tags.union(slideB.tags)
    val intersectionAScore = slideA.tags.intersect(slideB.tags)
    val intersectionBScore = slideB.tags.intersect(slideA.tags)
    return minOf(unionScore.size, intersectionAScore.size, intersectionBScore.size)
}