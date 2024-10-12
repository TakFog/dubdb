package takutility.dubdb.db

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import takutility.dubdb.entities.Dubber
import takutility.dubdb.entities.RawData
import takutility.dubdb.entities.SourceIds
import java.time.LocalDate

internal abstract class DubberRepositoryTest<R: DubberRepository>: RepositoryTest<Dubber, R>() {

    override fun newEntity(name: String, ids: SourceIds, parsed: Boolean, sources: MutableList<RawData>) = Dubber(
        name = name, ids = ids, parsed = parsed, sources = sources
    )

    @Test
    fun findMostRecent() {
        repo.save(Dubber("Tatiana Glass", lastUpdate = LocalDate.parse("2023-12-31")))
        repo.save(Dubber("Colette Kirk", lastUpdate = LocalDate.parse("2023-12-11")))
        val top4 = repo.save(Dubber("Zelenia Miller", lastUpdate = LocalDate.parse("2024-06-25")))
        repo.save(Dubber("Beatrice Cherry", lastUpdate = LocalDate.parse("2024-03-13")))
        repo.save(Dubber("Wilma Lee"))
        repo.save(Dubber("Yuri Browning", lastUpdate = LocalDate.parse("2024-04-01")))
        val top2 = repo.save(Dubber("Harding Wiley", lastUpdate = LocalDate.parse("2024-08-18")))
        repo.save(Dubber("Wing Raymond", lastUpdate = LocalDate.parse("2024-05-14")))
        repo.save(Dubber("Dalton Slater", lastUpdate = LocalDate.parse("2024-01-03")))
        val top3 = repo.save(Dubber("Lenore Munoz", lastUpdate = LocalDate.parse("2024-08-16")))
        repo.save(Dubber("Doris Head", lastUpdate = LocalDate.parse("2024-02-14")))
        repo.save(Dubber("Boris David"))
        val top5 = repo.save(Dubber("Bert Clark", lastUpdate = LocalDate.parse("2024-05-25")))
        val top1 = repo.save(Dubber("Brody Stewart", lastUpdate = LocalDate.parse("2024-09-22")))
        repo.save(Dubber("Matthew Roberson", lastUpdate = LocalDate.parse("2024-01-29")))

        val mostRecent = repo.findMostRecent(5)

        assertEquals(5, mostRecent.size)
        assertEquals(listOf(top1, top2, top3, top4, top5), mostRecent)
    }

    @Test
    fun findMostRecentWithNull() {
        repo.save(Dubber("Tatiana Glass"))
        repo.save(Dubber("Colette Kirk"))
        repo.save(Dubber("Wilma Lee"))
        repo.save(Dubber("Yuri Browning"))
        val top2 = repo.save(Dubber("Harding Wiley", lastUpdate = LocalDate.parse("2024-08-18")))
        repo.save(Dubber("Wing Raymond"))
        repo.save(Dubber("Dalton Slater"))
        val top3 = repo.save(Dubber("Lenore Munoz", lastUpdate = LocalDate.parse("2024-08-16")))
        repo.save(Dubber("Boris David"))
        val top1 = repo.save(Dubber("Brody Stewart", lastUpdate = LocalDate.parse("2024-09-22")))
        repo.save(Dubber("Matthew Roberson"))

        val mostRecent = repo.findMostRecent(5)

        assertEquals(3, mostRecent.size)
        assertEquals(listOf(top1, top2, top3), mostRecent)
    }
}