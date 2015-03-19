package controllers;

import java.util.ArrayList;
import java.util.List;

import play.data.validation.Constraints;
import nl.idgis.publisher.domain.web.TiledLayer;

public class TiledLayerForm {
	
	private static final Integer META_WIDTH_DEFAULT     = 4;
	private static final Integer META_HEIGTH_DEFAULT    = 4;
	private static final Integer EXPIRE_CACHE_DEFAULT   = 0;
	private static final Integer EXPIER_CLIENTS_DEFAULT = 0;
	private static final Integer GUTTER_DEFAULT         = 0;

	private static final String GIF  = "image/gif";
	private static final String JPG  = "image/jpeg";
	private static final String PNG8 = "image/png; mode=8bit";
	private static final String PNG  = "image/png";
	
	private static final Boolean PNG_DEFAULT  = true;
	private static final Boolean PNG8_DEFAULT = false;
	private static final Boolean JPG_DEFAULT  = false;
	private static final Boolean GIF_DEFAULT  = false;

	private TiledLayer tiledLayer;
	private Boolean png  = false;
	private Boolean png8 = false;
	private Boolean jpg  = false;
	private Boolean gif  = false;

	@Constraints.Required (message = "web.application.page.tiledlayers.form.field.metatilingwidth.validation.required")
	@Constraints.Min (value = 1, message = "web.application.page.tiledlayers.form.field.metatilingwidth.validation.min")
	@Constraints.Max (value = 20, message = "web.application.page.tiledlayers.form.field.metatilingwidth.validation.max")
	private Integer metaWidth     = META_WIDTH_DEFAULT;
	@Constraints.Required (message = "web.application.page.tiledlayers.form.field.metatilingheight.validation.required")
	@Constraints.Min (value = 1, message = "web.application.page.tiledlayers.form.field.metatilingheight.validation.min")
	@Constraints.Max (value = 20, message = "web.application.page.tiledlayers.form.field.metatilingheight.validation.max")
	private Integer metaHeight    = META_HEIGTH_DEFAULT;
	@Constraints.Required (message = "web.application.page.tiledlayers.form.field.servercache.validation.required")
	private Integer expireCache   = EXPIRE_CACHE_DEFAULT;
	@Constraints.Required (message = "web.application.page.tiledlayers.form.field.clientcache.validation.required")
	private Integer expireClients = EXPIER_CLIENTS_DEFAULT;
	@Constraints.Required (message = "web.application.page.tiledlayers.form.field.gutter.validation.required")
	private Integer gutter        = GUTTER_DEFAULT;

	
	public TiledLayerForm(){
		super();
	}

	public TiledLayerForm(final TiledLayer tl){
		setTiledLayer(tl);
	}

	public TiledLayer getTiledLayer() {
		return new TiledLayer("", "", getMetaWidth(), getMetaHeight(), getExpireCache(), getExpireClients(), getGutter(), getMimeFormats() );
	}

	public void setTiledLayer(TiledLayer tiledLayer) {
		this.tiledLayer = tiledLayer;
		if (tiledLayer == null){
			this.metaWidth = META_WIDTH_DEFAULT;
			this.metaHeight = META_HEIGTH_DEFAULT;
			this.expireCache = EXPIRE_CACHE_DEFAULT;
			this.expireClients = EXPIER_CLIENTS_DEFAULT;
			this.gutter = GUTTER_DEFAULT;
			setMimeFormats(null);
		} else {
			this.metaWidth = tiledLayer.metaWidth() == null ? META_WIDTH_DEFAULT : tiledLayer.metaWidth();
			this.metaHeight = tiledLayer.metaHeight() == null ? META_HEIGTH_DEFAULT : tiledLayer.metaHeight();
			this.expireCache = tiledLayer.expireCache() == null ? EXPIRE_CACHE_DEFAULT : tiledLayer.expireCache();
			this.expireClients = tiledLayer.expireClients() == null ? EXPIER_CLIENTS_DEFAULT : tiledLayer.expireClients();
			this.gutter = tiledLayer.gutter() == null ? GUTTER_DEFAULT : tiledLayer.gutter();
			setMimeFormats(tiledLayer.mimeformats());
		}
	}


	public List<String> getMimeFormats() {
		List<String> mimeFormats = new ArrayList<String>();
		if (getPng()) mimeFormats.add(PNG);
		if (getPng8()) mimeFormats.add(PNG8);
		if (getJpg()) mimeFormats.add(JPG);
		if (getGif()) mimeFormats.add(GIF);
		return mimeFormats;
	}

	public void setMimeFormats(List<String> mimeFormats) {
		if (mimeFormats==null){ 
			setPng(PNG_DEFAULT);
			setPng8(PNG8_DEFAULT);
			setJpg(JPG_DEFAULT);
			setGif(GIF_DEFAULT);
		} else {
			setPng(false);
			setPng8(false);
			setJpg(false);
			setGif(false);
			for (String string : mimeFormats) {
				if (string.equals(PNG)) setPng(true);
				if (string.equals(PNG8)) setPng8(true);
				if (string.equals(JPG)) setJpg(true);
				if (string.equals(GIF)) setGif(true);
			}
		}
	}

	public Boolean getPng() {
		return png;
	}

	public void setPng(Boolean png) {
		this.png = png;
	}

	public Boolean getPng8() {
		return png8;
	}

	public void setPng8(Boolean png8) {
		this.png8 = png8;
	}

	public Boolean getJpg() {
		return jpg;
	}

	public void setJpg(Boolean jpg) {
		this.jpg = jpg;
	}

	public Boolean getGif() {
		return gif;
	}

	public void setGif(Boolean gif) {
		this.gif = gif;
	}

	public Integer getMetaWidth() {
		return metaWidth;
	}

	public void setMetaWidth(Integer metaWidth) {
		this.metaWidth = metaWidth;
	}

	public Integer getMetaHeight() {
		return metaHeight;
	}

	public void setMetaHeight(Integer metaHeight) {
		this.metaHeight = metaHeight;
	}

	public Integer getExpireCache() {
		return expireCache;
	}

	public void setExpireCache(Integer expireCache) {
		this.expireCache = expireCache;
	}

	public Integer getExpireClients() {
		return expireClients;
	}

	public void setExpireClients(Integer expireClients) {
		this.expireClients = expireClients;
	}

	public Integer getGutter() {
		return gutter;
	}

	public void setGutter(Integer gutter) {
		this.gutter = gutter;
	}

	@Override
	public String toString() {
		return "TiledLayerForm [tiledLayer="
				+ tiledLayer + ", png=" + png + ", png8=" + png8 + ", jpg=" + jpg + ", gif=" + gif + ", metaWidth="
				+ metaWidth + ", metaHeight=" + metaHeight + ", expireCache=" + expireCache + ", expireClients="
				+ expireClients + ", gutter=" + gutter + "]";
	}

	
}
