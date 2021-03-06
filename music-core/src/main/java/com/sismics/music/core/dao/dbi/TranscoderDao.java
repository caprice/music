package com.sismics.music.core.dao.dbi;

import com.sismics.music.core.dao.dbi.mapper.TranscoderMapper;
import com.sismics.music.core.model.dbi.Transcoder;
import com.sismics.util.context.ThreadLocalContext;

import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Transcoder DAO.
 * 
 * @author jtremeaux
 */
public class TranscoderDao {
    /**
     * Creates a new transcoder.
     * 
     * @param transcoder Transcoder to create
     * @return Transcoder ID
     */
    public String create(Transcoder transcoder) {
        // Init transcoder data
        transcoder.setId(UUID.randomUUID().toString());
        transcoder.setCreateDate(new Date());

        // Create transcoder
        final Handle handle = ThreadLocalContext.get().getHandle();
        handle.createStatement("insert into " +
                " T_TRANSCODER(TRN_ID_C, TRN_NAME_C, TRN_SOURCE_C, TRN_DESTINATION_C, TRN_STEP1_C, TRN_STEP2_C, TRN_CREATEDATE_D)" +
                " values(:id, :name, :source, :destination, :step1, :step2, :createDate)")
                .bind("id", transcoder.getId())
                .bind("name", transcoder.getName())
                .bind("source", transcoder.getSource())
                .bind("destination", transcoder.getDestination())
                .bind("step1", transcoder.getStep1())
                .bind("step2", transcoder.getStep2())
                .bind("createDate", new Timestamp(transcoder.getCreateDate().getTime()))
                .execute();

        return transcoder.getId();
    }
    
    /**
     * Updates a transcoder.
     * 
     * @param transcoder Transcoder to update
     * @return Updated transcoder
     */
    public Transcoder update(Transcoder transcoder) {
        final Handle handle = ThreadLocalContext.get().getHandle();
        handle.createStatement("update T_TRANSCODER t set " +
                " t.TRN_NAME_C = :name," +
                " t.TRN_SOURCE_C = :source, " +
                " t.TRN_DESTINATION_C = :destination, " +
                " t.TRN_STEP1_C = :step1, " +
                " t.TRN_STEP2_C = :step2" +
                " where t.TRN_ID_C = :id and t.TRN_DELETEDATE_D is null")
                .bind("id", transcoder.getId())
                .bind("name", transcoder.getName())
                .bind("source", transcoder.getSource())
                .bind("destination", transcoder.getDestination())
                .bind("step1", transcoder.getStep1())
                .bind("step2", transcoder.getStep2())
                .execute();

        return transcoder;
    }
    
    /**
     * Gets a transcoder by its ID.
     * 
     * @param id Transcoder ID
     * @return Transcoder
     */
    public Transcoder getActiveById(String id) {
        final Handle handle = ThreadLocalContext.get().getHandle();
        Query<Transcoder> q = handle.createQuery("select " + new TranscoderMapper().getJoinedColumns("t") +
                "  from T_TRANSCODER t" +
                "  where t.TRN_ID_C = :id and t.TRN_DELETEDATE_D is null")
                .bind("id", id)
                .mapTo(Transcoder.class);
        return q.first();
    }
    
    /**
     * Deletes a transcoder.
     * 
     * @param id Transcoder's ID
     */
    public void delete(String id) {
        final Handle handle = ThreadLocalContext.get().getHandle();
        handle.createStatement("update T_TRANSCODER t" +
                "  set t.TRN_DELETEDATE_D = :deleteDate" +
                "  where t.TRN_ID_C = :id and t.TRN_DELETEDATE_D is null")
                .bind("id", id)
                .bind("deleteDate", new Date())
                .execute();
    }

    /**
     * Returns the list of all transcoders.
     *
     * @return List of transcoders
     */
    public List<Transcoder> findAll() {
        Handle handle = ThreadLocalContext.get().getHandle();
        Query<Transcoder> q = handle.createQuery("select " + new TranscoderMapper().getJoinedColumns("t")+
                "  from T_TRANSCODER t " +
                "  where t.TRN_DELETEDATE_D is null" +
                "  order by t.TRN_NAME_C is null")
                .mapTo(Transcoder.class);
        return q.list();
    }
}
