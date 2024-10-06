package takutility.dubdb.db.codec

import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import takutility.dubdb.entities.Dubber
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val LAST_UPDATE = "lastUpdate"

class DubberCodec : EntityCodec<Dubber>() {
    override fun newInstance(): Dubber = Dubber("")

    override fun getEncoderClass(): Class<Dubber> = Dubber::class.java

    override fun encodeObject(w: BsonWriter, entity: Dubber, ctx: EncoderContext?) {
        super.encodeObject(w, entity, ctx)
        entity.lastUpdate?.let { w.writeString(LAST_UPDATE, it.format(DateTimeFormatter.ISO_DATE)) }
    }

    override fun decodeField(key: String, r: BsonReader, ctx: DecoderContext?, inst: Dubber) {
        if (key == LAST_UPDATE)
            inst.lastUpdate = LocalDate.parse(r.readString())
        else
            super.decodeField(key, r, ctx, inst)
    }
}