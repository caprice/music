package com.sismics.music.rest.resource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;

import com.sismics.music.core.dao.dbi.AlbumDao;
import com.sismics.music.core.dao.dbi.TrackDao;
import com.sismics.music.core.dao.dbi.criteria.AlbumCriteria;
import com.sismics.music.core.dao.dbi.criteria.TrackCriteria;
import com.sismics.music.core.dao.dbi.dto.AlbumDto;
import com.sismics.music.core.dao.dbi.dto.TrackDto;
import com.sismics.music.core.model.context.AppContext;
import com.sismics.music.core.model.dbi.Album;
import com.sismics.music.core.service.albumart.AlbumArtService;
import com.sismics.music.core.service.albumart.AlbumArtSize;
import com.sismics.music.core.util.dbi.PaginatedList;
import com.sismics.music.core.util.dbi.PaginatedLists;
import com.sismics.music.core.util.dbi.SortCriteria;
import com.sismics.music.rest.util.JsonUtil;
import com.sismics.rest.exception.ClientException;
import com.sismics.rest.exception.ForbiddenClientException;

/**
 * Album REST resources.
 * 
 * @author jtremeaux
 */
@Path("/album")
public class AlbumResource extends BaseResource {
    /**
     * Returns an album detail.
     *
     * @return Response
     */
    @GET
    @Path("{id: [a-z0-9\\-]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response detail(
            @PathParam("id") String id) {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }

        // Get album info
        AlbumDao albumDao = new AlbumDao();
        List<AlbumDto> albumList = albumDao.findByCriteria(new AlbumCriteria().setUserId(principal.getId()).setId(id));
        if (albumList.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        AlbumDto album = albumList.iterator().next();

        JsonObjectBuilder response = Json.createObjectBuilder()
                .add("id", album.getId())
                .add("name", album.getName())
                .add("albumart", album.getAlbumArt() != null)
                .add("play_count", album.getUserPlayCount());
        
        response.add("artist", Json.createObjectBuilder()
                .add("id", album.getArtistId())
                .add("name", album.getArtistName()));

        // Get track info
        JsonArrayBuilder tracks = Json.createArrayBuilder();
        TrackDao trackDao = new TrackDao();
        List<TrackDto> trackList = trackDao.findByCriteria(new TrackCriteria()
                .setAlbumId(album.getId())
                .setUserId(principal.getId()));
        
        for (TrackDto trackDto : trackList) {
            tracks.add(Json.createObjectBuilder()
                    .add("order", JsonUtil.nullable(trackDto.getOrder()))
                    .add("id", trackDto.getId())
                    .add("title", trackDto.getTitle())
                    .add("year", JsonUtil.nullable(trackDto.getYear()))
                    .add("genre", JsonUtil.nullable(trackDto.getGenre()))
                    .add("length", trackDto.getLength())
                    .add("bitrate", trackDto.getBitrate())
                    .add("vbr", trackDto.isVbr())
                    .add("format", trackDto.getFormat())
                    .add("filename", trackDto.getFileName())
                    .add("play_count", trackDto.getUserTrackPlayCount())
                    .add("liked", trackDto.isUserTrackLike())
                    .add("artist", Json.createObjectBuilder()
                            .add("id", trackDto.getArtistId())
                            .add("name", trackDto.getArtistName())));
        }
        response.add("tracks", tracks);

        return Response.ok().entity(response.build()).build();
    }

    /**
     * Returns an album cover.
     *
     * @param id Album ID
     * @param size Cover size
     * @return Response
     */
    @GET
    @Path("{id: [a-z0-9\\-]+}/albumart/{size: [a-z]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response albumart(
            @PathParam("id") String id,
            @PathParam("size") String size) {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }

        // Get the album
        AlbumDao albumDao = new AlbumDao();
        Album album = albumDao.getActiveById(id);
        if (album == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Get the album art size
        AlbumArtSize albumArtSize = null;
        try {
            albumArtSize = AlbumArtSize.valueOf(size.toUpperCase());
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Get the file
        final AlbumArtService albumArtService = AppContext.getInstance().getAlbumArtService();
        File file = albumArtService.getAlbumArtFile(album.getAlbumArt(), albumArtSize);
        if (!file.exists() || !file.canRead()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(file, "image/jpeg")
                .header("Expires", new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z").format(new Date().getTime() + 3600000 * 24 * 7))
                .build();
    }
    
    /**
     * Update an album cover.
     *
     * @param id Album ID
     * @param url Image URL
     * @return Response
     */
    @POST
    @Path("{id: [a-z0-9\\-]+}/albumart")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateAlbumart(
            @PathParam("id") String id,
            @FormParam("url") String url) {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }

        // Get the album
        AlbumDao albumDao = new AlbumDao();
        Album album = albumDao.getActiveById(id);
        if (album == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Copy the remote URL to a temporary file
        File imageFile = null;
        try {
            imageFile = File.createTempFile("music_albumart", null);
            try (InputStream urlStream = new URL(url).openStream();
                    OutputStream imageStream = new FileOutputStream(imageFile)) {
                IOUtils.copy(urlStream, imageStream);
            }
        } catch (IOException e) {
            throw new ClientException("IOError", "Error while reading the remote URL", e);
        }
            
        // Update the album art
        final AlbumArtService albumArtService = AppContext.getInstance().getAlbumArtService();
        String oldAlbumArtId = album.getAlbumArt();
        try {
            String albumArtId = albumArtService.importAlbumArt(imageFile);
            album.setAlbumArt(albumArtId);
            albumDao.update(album);
        } catch (Exception e) {
            throw new ClientException("ImageError", "The provided URL is not an image", e);
        }
        
        // Delete the previous album art
        if (oldAlbumArtId != null) {
            albumArtService.deleteAlbumArt(oldAlbumArtId);
        }

        // Always return OK
        return Response.ok()
                .entity(Json.createObjectBuilder().add("status", "ok").build())
                .build();
    }

    /**
     * Returns all active albums.
     *
     * @return Response
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(
            @QueryParam("limit") Integer limit,
            @QueryParam("offset") Integer offset,
            @QueryParam("sort_column") Integer sortColumn,
            @QueryParam("asc") Boolean asc,
            @QueryParam("search") String search) {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }

        AlbumDao albumDao = new AlbumDao();
        PaginatedList<AlbumDto> paginatedList = PaginatedLists.create(limit, offset);
        SortCriteria sortCriteria = new SortCriteria(sortColumn, asc);
        AlbumCriteria albumCriteria = new AlbumCriteria()
                .setUserId(principal.getId())
                .setNameLike(search);
        albumDao.findByCriteria(paginatedList, albumCriteria, sortCriteria);

        JsonObjectBuilder response = Json.createObjectBuilder();
        JsonArrayBuilder items = Json.createArrayBuilder();
        for (AlbumDto album : paginatedList.getResultList()) {
            items.add(Json.createObjectBuilder()
                    .add("id", album.getId())
                    .add("name", album.getName())
                    .add("update_date", album.getUpdateDate().getTime())
                    .add("albumart", album.getAlbumArt() != null)
                    .add("play_count", album.getUserPlayCount())
                    .add("artist", Json.createObjectBuilder()
                            .add("id", album.getArtistId())
                            .add("name", album.getArtistName())));
        }
        
        response.add("total", paginatedList.getResultCount());
        response.add("albums", items);

        return Response.ok().entity(response.build()).build();
    }
}
