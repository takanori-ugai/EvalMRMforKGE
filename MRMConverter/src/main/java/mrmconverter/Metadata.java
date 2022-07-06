package mrmconverter;

import java.util.Map;

public class Metadata {
	String scene;
	String subj;
	String pred;
	String obj;
	Map<String,String> metadata;
	
	
	public Metadata(String scene, String subj, String pred, String obj, Map<String, String> metadata) {
		super();
		this.scene = scene;
		this.subj = subj;
		this.pred = pred;
		this.obj = obj;
		this.metadata = metadata;
	}


	public String getScene() {
		return scene;
	}


	public void setScene(String scene) {
		this.scene = scene;
	}


	public String getSubj() {
		return subj;
	}


	public void setSubj(String subj) {
		this.subj = subj;
	}


	public String getPred() {
		return pred;
	}


	public void setPred(String pred) {
		this.pred = pred;
	}


	public String getObj() {
		return obj;
	}


	public void setObj(String obj) {
		this.obj = obj;
	}


	public Map<String, String> getMetadata() {
		return metadata;
	}


	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}
	

	
}
