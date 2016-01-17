/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * GVoD is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.kth.ws.gvod;

import com.google.common.primitives.Ints;
import se.sics.gvod.manager.toolbox.GVoDSyncI;
import com.google.common.util.concurrent.SettableFuture;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.ws.gvod.model.PlayResponseJSON;
import se.kth.ws.gvod.model.VideoOpErrorJSON;
import se.sics.gvod.common.util.VoDHeartbeatServiceEnum;
import se.sics.gvod.manager.util.FileStatus;
import se.sics.gvod.manager.toolbox.Result;
import se.sics.gvod.manager.toolbox.VideoInfo;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.IntIdentifier;
import se.sics.ws.gvod.util.ResponseStatusWSMapper;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class GVoDRESTMsgs {

    private static final Logger LOG = LoggerFactory.getLogger(GVoDWS.class);

    @Path("/files")
    @Produces(MediaType.APPLICATION_JSON)
    public static class FilesResource {

        private GVoDSyncI gvod;

        public FilesResource(GVoDSyncI gvod) {
            this.gvod = gvod;
        }

        @GET
        public Response getFiles() {
            LOG.info("received get files request");
            if(!gvod.isReady()) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("gvod not ready").build();
            }
            
            SettableFuture<Result<Map<String, FileStatus>>> myFuture = SettableFuture.create();
            gvod.getFiles(myFuture);
            try {
                Result<Map<String, FileStatus>> result = myFuture.get();
                LOG.info("sending get files response:{}", result.status.toString());
                if (result.ok()) {
                    return Response.status(Response.Status.OK).entity(result.value.get()).build();
                } else {
                    return Response.status(ResponseStatusWSMapper.map(result.status)).entity(result.getDetails()).build();
                }

            } catch (InterruptedException ex) {
                LOG.error("sending get files response:{}", Response.Status.INTERNAL_SERVER_ERROR);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("ws internal error").build();
            } catch (ExecutionException ex) {
                LOG.error("sending get files response:{}", Response.Status.INTERNAL_SERVER_ERROR);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("ws internal error").build();
            }
        }

    }

    @Path("/library")
    @Produces(MediaType.APPLICATION_JSON)
    public static class LibraryResource {

        private GVoDSyncI gvod;

        public LibraryResource(GVoDSyncI gvod) {
            this.gvod = gvod;
        }

        @GET
        public Response refreshLibrary() {
            LOG.info("received refresh library request");
            
            if(!gvod.isReady()) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("gvod not ready").build();
            }

            SettableFuture<Result<Map<String, FileStatus>>> myFuture = SettableFuture.create();
            gvod.getFiles(myFuture);
            try {
                Result<Map<String, FileStatus>> result = myFuture.get();
                LOG.info("sending refresh library response:{}", result.status.toString());
                if (result.ok()) {
                    return Response.status(Response.Status.OK).entity(true).build();
                } else {
                    return Response.status(ResponseStatusWSMapper.map(result.status)).entity(result.getDetails()).build();
                }
            } catch (InterruptedException ex) {
                LOG.error("sending refresh library response:{}", Response.Status.INTERNAL_SERVER_ERROR);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("ws internal error").build();
            } catch (ExecutionException ex) {
                LOG.error("sending refresh library response:{}", Response.Status.INTERNAL_SERVER_ERROR);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("ws internal error").build();
            }
        }
    }

    @Path("/pendinguploadvideo")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public static class PendingUploadResource {

        private GVoDSyncI gvod;

        public PendingUploadResource(GVoDSyncI gvod) {
            this.gvod = gvod;
        }

        @PUT
        public Response pendingUpload(VideoInfo videoInfo) {
            LOG.info("received pending upload request for:{}", videoInfo.getName());
            
            if(!gvod.isReady()) {
                VideoOpErrorJSON errorDesc = new VideoOpErrorJSON(videoInfo, "gvod not ready");
                return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(errorDesc).build();
            }

            SettableFuture<Result<Boolean>> myFuture = SettableFuture.create();
            gvod.pendingUpload(videoInfo, myFuture);

            VideoInfo ret = new VideoInfo();
            ret.setName(videoInfo.getName());
            ret.setOverlayId(getRandomOverlayId());

            try {
                Result<Boolean> result = myFuture.get();
                LOG.info("sending pending upload response:{}", result.status.toString());
                if (result.ok()) {
                    return Response.status(Response.Status.OK).entity(ret).build();
                } else {
                    VideoOpErrorJSON errorDesc = new VideoOpErrorJSON(videoInfo, result.getDetails());
                    return Response.status(ResponseStatusWSMapper.map(result.status)).entity(errorDesc).build();
                }
            } catch (InterruptedException ex) {
                LOG.error("sending pending upload response:{}", Response.Status.INTERNAL_SERVER_ERROR);
                VideoOpErrorJSON errorDesc = new VideoOpErrorJSON(videoInfo, "ws internal error");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorDesc).build();
            } catch (ExecutionException ex) {
                LOG.error("sending pending upload response:{}", Response.Status.INTERNAL_SERVER_ERROR);
                VideoOpErrorJSON errorDesc = new VideoOpErrorJSON(videoInfo, "ws internal error");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorDesc).build();
            }
        }
        
        private Identifier getRandomOverlayId() {
            Random rand = new SecureRandom();
            byte[] randBytes = new byte[3];
            rand.nextBytes(randBytes);
            int overlayId = Ints.fromBytes(VoDHeartbeatServiceEnum.CROUPIER.getServiceId(), randBytes[0], randBytes[1], randBytes[2]);
            return new IntIdentifier(overlayId);
        }
    }

    @Path("/uploadvideo")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public static class UploadResource {

        private GVoDSyncI gvod;

        public UploadResource(GVoDSyncI gvod) {
            this.gvod = gvod;
        }

        @PUT
        public Response uploadVideo(VideoInfo videoInfo) {
            LOG.info("received upload requestfor:{}", videoInfo.getName());

            if(!gvod.isReady()) {
                VideoOpErrorJSON errorDesc = new VideoOpErrorJSON(videoInfo, "gvod not ready");
                return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(errorDesc).build();
            }

            SettableFuture<Result<Boolean>> myFuture = SettableFuture.create();
            gvod.upload(videoInfo, myFuture);
            try {
                Result<Boolean> result = myFuture.get();
                LOG.info("sending upload response:{}", result.status.toString());
                if (result.ok()) {
                    return Response.status(Response.Status.OK).entity(videoInfo).build();
                } else {
                    VideoOpErrorJSON errorDesc = new VideoOpErrorJSON(videoInfo, result.getDetails());
                    return Response.status(ResponseStatusWSMapper.map(result.status)).entity(errorDesc).build();
                }
            } catch (InterruptedException ex) {
                LOG.error("sending upload response:{}", Response.Status.INTERNAL_SERVER_ERROR);
                VideoOpErrorJSON errorDesc = new VideoOpErrorJSON(videoInfo, "ws internal error");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorDesc).build();
            } catch (ExecutionException ex) {
                LOG.error("sending upload response:{}", Response.Status.INTERNAL_SERVER_ERROR);
                VideoOpErrorJSON errorDesc = new VideoOpErrorJSON(videoInfo, "ws internal error");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorDesc).build();
            }
        }
    }

    @Path("/downloadvideo")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public static class DownloadResource {

        private GVoDSyncI gvod;

        public DownloadResource(GVoDSyncI gvod) {
            this.gvod = gvod;
        }

        @PUT
        public Response downloadVideo(VideoInfo videoInfo) {
            LOG.info("received download request for:{}", videoInfo.getName());
            
            if(!gvod.isReady()) {
                VideoOpErrorJSON errorDesc = new VideoOpErrorJSON(videoInfo, "gvod not ready");
                return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(errorDesc).build();
            }


            SettableFuture<Result<Boolean>> myFuture = SettableFuture.create();
            gvod.download(videoInfo, myFuture);
            try {
                Result<Boolean> result = myFuture.get();
                LOG.info("sending download response:{}", result.status.toString());
                if (result.ok()) {
                    return Response.status(Response.Status.OK).entity(videoInfo).build();
                } else {
                    VideoOpErrorJSON errorDesc = new VideoOpErrorJSON(videoInfo, result.getDetails());
                    return Response.status(ResponseStatusWSMapper.map(result.status)).entity(errorDesc).build();
                }
            } catch (InterruptedException ex) {
                LOG.error("sending download response:{}", Response.Status.INTERNAL_SERVER_ERROR);
                VideoOpErrorJSON errorDesc = new VideoOpErrorJSON(videoInfo, "ws internal error");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorDesc).build();
            } catch (ExecutionException ex) {
                LOG.error("sending download response:{}", Response.Status.INTERNAL_SERVER_ERROR);
                VideoOpErrorJSON errorDesc = new VideoOpErrorJSON(videoInfo, "ws internal error");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorDesc).build();
            }
        }
    }

    @Path("/removevideo")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public static class RemoveResource {

        private GVoDSyncI gvod;

        public RemoveResource(GVoDSyncI gvod) {
            this.gvod = gvod;
        }

        @PUT
        public Response removeVideo(VideoInfo videoInfo) {
            LOG.info("received remove request for:{}", videoInfo.getName());
            if(!gvod.isReady()) {
                VideoOpErrorJSON errorDesc = new VideoOpErrorJSON(videoInfo, "gvod not ready");
                return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(errorDesc).build();
            }


            SettableFuture<Result<Boolean>> myFuture = SettableFuture.create();

            //TODO Alex actual component shutdown and cleanup
            myFuture.set(new Result(true));

            try {
                Result<Boolean> result = myFuture.get();
                LOG.info("sending remove response:{}", result.status.toString());
                if (result.ok()) {
                    return Response.status(Response.Status.OK).entity(videoInfo).build();
                } else {
                    VideoOpErrorJSON errorDesc = new VideoOpErrorJSON(videoInfo, result.getDetails());
                    return Response.status(ResponseStatusWSMapper.map(result.status)).entity(errorDesc).build();
                }
            } catch (InterruptedException ex) {
                LOG.error("sending remove response:{}", Response.Status.INTERNAL_SERVER_ERROR);
                VideoOpErrorJSON errorDesc = new VideoOpErrorJSON(videoInfo, "ws internal error");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorDesc).build();
            } catch (ExecutionException ex) {
                LOG.error("sending remove response:{}", Response.Status.INTERNAL_SERVER_ERROR);
                VideoOpErrorJSON errorDesc = new VideoOpErrorJSON(videoInfo, "ws internal error");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorDesc).build();
            }
        }
    }

    @Path("/play")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public static class PlayVideoResource {

        private GVoDSyncI gvod;

        public PlayVideoResource(GVoDSyncI gvod) {
            this.gvod = gvod;
        }

        @PUT
        public Response playVideo(VideoInfo videoInfo) {
            LOG.info("received play request for:{}", videoInfo.getName());
            
            if(!gvod.isReady()) {
                VideoOpErrorJSON errorDesc = new VideoOpErrorJSON(videoInfo, "gvod not ready");
                return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(errorDesc).build();
            }

            SettableFuture<Result<Integer>> myFuture = SettableFuture.create();
            gvod.play(videoInfo, myFuture);
            try {
                Result<Integer> result = myFuture.get();

                LOG.info("sending play response:{}", result.status.toString());
                if (result.ok()) {
                    return Response.status(Response.Status.OK).entity(new PlayResponseJSON(videoInfo, result.value.get())).build();
                } else {
                    VideoOpErrorJSON errorDesc = new VideoOpErrorJSON(videoInfo, result.getDetails());
                    return Response.status(ResponseStatusWSMapper.map(result.status)).entity(errorDesc).build();
                }
            } catch (InterruptedException ex) {
                LOG.error("sending play response:{}", Response.Status.INTERNAL_SERVER_ERROR);
                VideoOpErrorJSON errorDesc = new VideoOpErrorJSON(videoInfo, "ws internal error");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorDesc).build();
            } catch (ExecutionException ex) {
                LOG.error("sending play response:{}", Response.Status.INTERNAL_SERVER_ERROR);
                VideoOpErrorJSON errorDesc = new VideoOpErrorJSON(videoInfo, "ws internal error");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorDesc).build();
            }
        }
    }

    @Path("/stop")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public static class StopVideoResource {
        private GVoDSyncI gvod;

        public StopVideoResource(GVoDSyncI gvod) {
            this.gvod = gvod;
        }

        @PUT
        public Response stopVideo(VideoInfo videoInfo) {
            LOG.info("received stop request for:{}", videoInfo.getName());
            
            if(!gvod.isReady()) {
                VideoOpErrorJSON errorDesc = new VideoOpErrorJSON(videoInfo, "gvod not ready");
                return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(errorDesc).build();
            }

            SettableFuture<Result<Boolean>> myFuture = SettableFuture.create();
            gvod.stop(videoInfo, myFuture);
            
            try {
                Result<Boolean> result = myFuture.get();

                LOG.info("sending stop response:{}", result.status.toString());
                if (result.ok()) {
                    return Response.status(Response.Status.OK).entity(videoInfo).build();
                } else {
                    VideoOpErrorJSON errorDesc = new VideoOpErrorJSON(videoInfo, result.getDetails());
                    return Response.status(ResponseStatusWSMapper.map(result.status)).entity(errorDesc).build();
                }
            } catch (InterruptedException ex) {
                LOG.error("sending upload response:{}", Response.Status.INTERNAL_SERVER_ERROR);
                VideoOpErrorJSON errorDesc = new VideoOpErrorJSON(videoInfo, "ws internal error");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorDesc).build();
            } catch (ExecutionException ex) {
                LOG.error("sending upload response:{}", Response.Status.INTERNAL_SERVER_ERROR);
                VideoOpErrorJSON errorDesc = new VideoOpErrorJSON(videoInfo, "ws internal error");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorDesc).build();
            }
        }
    }

    @Path("/caracalStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public static class  GetCaracalStatus {

        private AtomicBoolean isServerDown = new AtomicBoolean();

        public GetCaracalStatus(){

            this.isServerDown = new AtomicBoolean();
            this.isServerDown.set(false);
        }

        public void setIsServerDown(boolean status){
            this.isServerDown.set(status);
        }

        @GET
        public Response caracalStatus(){

            LOG.info("Received request to return the status of the caracal server.");
            return Response.status(Response.Status.OK).entity(isServerDown.get()).build();
        }

    }

}
