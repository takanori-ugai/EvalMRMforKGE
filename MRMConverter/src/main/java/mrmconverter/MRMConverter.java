package mrmconverter;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.system.RDFStar;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;

public class MRMConverter {
	final static String kgc = "http://kgc.knowledge-graph.jp/ontology/kgc.owl#";
	static Map<String, Metadata> id_map = new HashMap<String,Metadata>();
	static String[] bugs = {
//			"http://kgc.knowledge-graph.jp/data/ACaseOfIdentity/489",
//			"http://kgc.knowledge-graph.jp/data/ACaseOfIdentity/546",
//			"http://kgc.knowledge-graph.jp/data/ACaseOfIdentity/312",
//			"http://kgc.knowledge-graph.jp/data/ACaseOfIdentity/161",
//			"http://kgc.knowledge-graph.jp/data/ACaseOfIdentity/526",
//			"http://kgc.knowledge-graph.jp/data/SilverBlaze/381",
//			"http://kgc.knowledge-graph.jp/data/SilverBlaze/380",
//			"http://kgc.knowledge-graph.jp/data/ACaseOfIdentity/437",
//			"http://kgc.knowledge-graph.jp/data/ACaseOfIdentity/502"
			};
	
	public static Model rdfLoader(String file_path) {
		Model model = ModelFactory.createDefaultModel();
		try {
			FileInputStream file = new FileInputStream(file_path);
			if (file_path.contains(".nt")) {
				model.read(file, null, "NT");
			} else if (file_path.contains(".ttl")) {
				model.read(file, null, "TTL");
			} else if (file_path.contains(".rdf")) {
				model.read(file, null, "RDF/XML");
			} else if (file_path.contains(".jsonld")) {
				model.read(file, null, "JSON-LD");
			} else {
				model.read(file, null, "TTL");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return model;
	}
	
	public static Map<String, ArrayList<Metadata>> executeQuery(String queryStr, Model model) {
		Map<String, ArrayList<Metadata>> stmt_map = new HashMap<String, ArrayList<Metadata>>();	//key:scene, val:list<Metadata> .Scenes can have multiple subjects and multiple objects, so each is devided and added to the list.
		Query query = QueryFactory.create(queryStr);
		QueryExecution qe = QueryExecutionFactory.create(query, model);
		ResultSet results = qe.execSelect();
//		ResultSetFormatter.out(System.out, results, query);
		while (results.hasNext()) {
			Map<String,String> metadata = new HashMap<String,String>();
			String scene = "";
			String subj = "";
			String pred = "";
			String obj = "";
			QuerySolution result = results.next();
			scene = result.get("s").toString();
			RDFNode p = result.get("p");
			String p_str = "";
			
			RDFNode who = result.get("who");
			if (who != null) {
				subj = who.toString();
			} else {
				subj = OWL.Nothing.getURI();
			}
			RDFNode predicate = result.get("predicate");
			String predicate_str = "";
			
			if (p != null) {
				p_str = p.toString();
				predicate_str = predicate.toString();
			} else {
				p_str = OWL.NS + "TopObjectProperty";
			}
			
			RDFNode what = result.get("what");
			
			
			String obj_type = "";
			
			// hasProperty
			if (predicate_str.equals(kgc + "hasProeprty")) {
				pred = predicate_str;
				obj = p_str;
			}
			// hasPredicate
			else {
				pred = p_str;
				
				if(what != null) {
					obj = what.toString();
				} else {
					if (result.contains("whom")) { obj = result.get("whom").toString(); obj_type = "whom";}
					else if (result.contains("where")) { obj = result.get("where").toString(); obj_type = "where"; }
					else if (result.contains("on")) { obj = result.get("on").toString(); obj_type = "on";}
					else if (result.contains("to")) { obj = result.get("to").toString(); obj_type = "to";}
					else if (result.contains("from")) { obj = result.get("from").toString(); obj_type = "from";}
					else { obj = OWL.Nothing.getURI(); }
				}
			} 
			 
			
			Iterator<String> names = result.varNames();
			while (names.hasNext()) {
				String vn = names.next();
				if (vn.equals("s") || vn.equals("who") || vn.equals("predicate") || vn.equals("p") || vn.equals("what") || vn.equals(obj_type)) {
					continue;
				} else {
					metadata.put(vn, result.get(vn).toString());
//					System.out.println("add: " + vn + " : " + result.get(vn));
				}
				
			}
			
			Metadata stmt = new Metadata(scene, subj, pred, obj, metadata);
			ArrayList<Metadata> list = new ArrayList<Metadata>();
			if (stmt_map.containsKey(scene)) {
				list = stmt_map.get(scene);
			}
			list.add(stmt);
			stmt_map.put(scene, list);
//			System.out.println("---");
		}
		qe.close();

		return stmt_map;
	}
	
	public static RDFNode getRDFStarStatement(String scene, Model model) {
		String sparql = "PREFIX kgc: <http://kgc.knowledge-graph.jp/ontology/kgc.owl#>\n"
				+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
				+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
				+ "SELECT DISTINCT * WHERE {\n"
				+ "?s rdf:value \"" + scene + "\" .\n"
				+ "}";
		Query query = QueryFactory.create(sparql);
		QueryExecution qe = QueryExecutionFactory.create(query, model);
		ResultSet results = qe.execSelect();
		RDFNode s = null;
		while (results.hasNext()) {
			QuerySolution result = results.next();
			s = result.get("s");
		}
		return s;
	}
	
	public static ArrayList<Resource> expandNestStatement(Map<String,ArrayList<Metadata>> stmt_map, String obj, Model model, String mode) {
		ArrayList<Resource> obj_r_list = new ArrayList<Resource>();
		Resource obj_r = null;
		
		String[] uri_split = obj.split("/");
		String id = uri_split[uri_split.length-1];
		Pattern pattern = Pattern.compile("^[0-9]{3}$");
		Matcher matcher = pattern.matcher(id);
		
		// if obj is scene resource
		if (matcher.find() && !Arrays.asList(bugs).contains(obj)) {
//			System.out.println(obj);
			ArrayList<Metadata> _stmt_list = stmt_map.get(obj);
			try {
				for (Metadata _stmt : _stmt_list) {
					String _subj = _stmt.getSubj();
					String _pred = _stmt.getPred();
					String _obj = _stmt.getObj();
					String scene = _stmt.getScene();
					Resource _subj_r = model.getResource(_subj);
					Property _pred_r = model.getProperty(_pred);
					ArrayList<Resource> _obj_r_list = expandNestStatement(stmt_map, _obj, model, mode);
					for (Resource _obj_r : _obj_r_list) {
						obj_r = createRDFStarStatement(_subj_r, _pred_r, _obj_r, scene, model, mode); 
						obj_r_list.add(obj_r);
					}
				}
			} catch (Exception e) {
				// issues on the kg side
//				e.printStackTrace();
				obj_r = model.getResource(obj);
				obj_r_list.add(obj_r);
			}
		} else {
			obj_r = model.getResource(obj);
			obj_r_list.add(obj_r);
		}
		return obj_r_list;
	}
	
	public static Resource createRDFStarStatement(Resource subj_r, Property pred_r, Resource obj_r, String scene, Model model, String mode) {
		Statement _rdf_stmt = model.createStatement(subj_r, pred_r, obj_r);
		Resource _rdf_stmt_r = model.createResource(_rdf_stmt);
		Statement rdf_stmt = null;
		Resource rdf_stmt_r = null;

		// Not distinguish quoted triples
		if (mode.equals("0")) {
			rdf_stmt_r = _rdf_stmt_r;
			rdf_stmt_r.addProperty(RDF.value, model.createLiteral(scene));
		} 
		// Distinguish quoted triples by id
		else {
			rdf_stmt = model.createStatement(_rdf_stmt_r, RDF.value, scene);
			rdf_stmt_r = model.createResource(rdf_stmt);
		}
		
		model.removeAll(model.getResource(scene), null, null);
		
		return rdf_stmt_r;
	}
	
	public static void main(String[] args) {
		String file_path = args[0];
		String mode = args[1];
//		String source_type = args[1]; // source MRM
//		String target_type = args[2]; // target MRM
		try {
			Model model = rdfLoader(file_path);
			//SPARQLをかけてMetadata, p, oを取り出す。
			String sparql = "PREFIX kgc: <http://kgc.knowledge-graph.jp/ontology/kgc.owl#>\n"
					+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
					+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
					+ "SELECT DISTINCT * WHERE { \n"
					+ "	?s rdf:type/rdfs:subClassOf kgc:Scene .\n"
					+ "    ?s rdf:type ?type ."
					+ "        \n"
					+ "    optional {?s ?predicate ?p filter (?predicate = kgc:hasPredicate || ?predicate = kgc:hasProperty)}"
					+ "    optional { ?s kgc:subject ?who filter(isURI(?who))}\n"
					+ "    optional { ?s kgc:what ?what filter(isURI(?what))}\n"
					+ "    optional { ?s kgc:where ?where . filter(isURI(?where))}\n"
					+ "    optional { ?s kgc:when ?when . filter(isURI(?when))}\n"
					+ "    optional { ?s kgc:infoSource ?infoSource . filter(isURI(?infoSource))}\n"
					+ "    optional { ?s kgc:then ?then . filter(isURI(?then))}\n"
					+ "    optional { ?s kgc:time ?time . filter(isURI(?time))}\n"
					+ "    optional { ?s kgc:to ?to . filter(isURI(?to))}\n"
					+ "    optional { ?s kgc:because ?because . filter(isURI(?because))}\n"
					+ "    optional { ?s kgc:near ?near . filter(isURI(?near))}\n"
					+ "    optional { ?s kgc:from ?from . filter(isURI(?from))}\n"
					+ "    optional { ?s kgc:how ?how . filter(isURI(?how))}\n"
					+ "    optional { ?s kgc:on ?on . filter(isURI(?on))}\n"
					+ "    optional { ?s kgc:why ?why . filter(isURI(?why))}\n"
					+ "    optional { ?s kgc:if ?if . filter(isURI(?if))}\n"
					+ "    optional { ?s kgc:at_the_same_time ?at_the_same_time . filter(isURI(?at_the_same_time))}\n"
					+ "    optional { ?s kgc:whom ?whom . filter(isURI(?whom))}\n"
					+ "    optional { ?s kgc:begin ?begin . filter(isURI(?begin))}\n"
					+ "    optional { ?s kgc:nextTo ?nextTo . filter(isURI(?nextTo))}\n"
					+ "    optional { ?s kgc:end ?end . filter(isURI(?end))}\n"
					+ "    optional { ?s kgc:middleOf ?middleOf . filter(isURI(?middleOf))}\n"
					+ "    optional { ?s kgc:opposite ?opposite . filter(isURI(?opposite))}\n"
					+ "    optional { ?s kgc:under ?under . filter(isURI(?under))}\n"
					+ "    optional { ?s kgc:left ?left . filter(isURI(?left))}\n"
					+ "    optional { ?s kgc:right ?right . filter(isURI(?right))}\n"
					+ "    optional { ?s kgc:therefore ?therefore . filter(isURI(?therefore))}\n"
					+ "    optional { ?s kgc:next_to ?next_to . filter(isURI(?next_to))}\n"
					+ "    optional { ?s kgc:adjunct ?adjunct . filter(isURI(?adjunct))}\n"
					+ "    optional { ?s kgc:middle ?middle . filter(isURI(?middle))}\n"
					+ "    optional { ?s kgc:close ?close . filter(isURI(?close))}\n"
					+ "    optional { ?s kgc:after ?after . filter(isURI(?after))}\n"
					+ "    optional { ?s kgc:and ?and . filter(isURI(?and))}\n"
					+ "    optional { ?s kgc:however ?however . filter(isURI(?however))}\n"
					+ "    optional { ?s kgc:whoｍ ?who_m . filter(isURI(?who_m))}\n"
					+ "    optional { ?s kgc:before ?before . filter(isURI(?before))}\n"
					+ "    optional { ?s kgc:hasPart ?hasPart . filter(isURI(?hasPart))}\n"
					+ "    optional { ?s kgc:otherwise ?otherwise . filter(isURI(?otherwise))}\n"
					+ "    optional { ?s kgc:infoReceiver ?infoReceiver . filter(isURI(?infoReceiver))}\n"
					+ "}";
			
			
			Map<String, ArrayList<Metadata>> stmt_map = executeQuery(sparql, model);
			
			for (Entry<String, ArrayList<Metadata>> e : stmt_map.entrySet()) {
				
				String scene = e.getKey();
				ArrayList<Metadata> stmt_list = e.getValue();
				
				for (Metadata stmt : stmt_list) {
					String subj = stmt.getSubj();
					String pred = stmt.getPred();
					String obj = stmt.getObj();
					Map<String,String> metadata = stmt.getMetadata();
					
					if (model.contains(null, RDF.value, scene)) {
						//The statement resource has been already created.
						continue;
					}
					
					Resource subj_r = model.getResource(subj);
					Property pred_r = model.getProperty(pred);
					ArrayList<Resource> obj_r_list = expandNestStatement(stmt_map, obj, model, mode);
					
					// There can be more than one object.
					for (Resource obj_r : obj_r_list) {
						Resource rdf_stmt_r = createRDFStarStatement(subj_r, pred_r, obj_r, scene, model, mode);

						// Metadata of RDFStar statement resource
						for (Entry<String,String> e2 : metadata.entrySet()) {
							String key = e2.getKey();
							Property key_p = model.getProperty(kgc + key);
							String val = e2.getValue();
							ArrayList<Resource> val_r_list = expandNestStatement(stmt_map, val, model, mode);
							for (Resource val_r : val_r_list) {
								rdf_stmt_r.addProperty(key_p, val_r);
							}
						}
					}
				}
				
			}
			
			model.setNsPrefix("owl", OWL.NS);
			model.setNsPrefix("rdfs", RDFS.uri);
			model.setNsPrefix("kgc", kgc);
			model.setNsPrefix("kdp", "http://kgc.knowledge-graph.jp/data/predicate/");
			model.setNsPrefix("kddf", "http://kgc.knowledge-graph.jp/data/DevilsFoot/");
			model.setNsPrefix("kdag", "http://kgc.knowledge-graph.jp/data/AbbeyGrange/");
			model.setNsPrefix("kdsl", "http://kgc.knowledge-graph.jp/data/SilverBlaze/");
			model.setNsPrefix("kdci", "http://kgc.knowledge-graph.jp/data/ACaseOfIdentity/");
			model.setNsPrefix("kdrp", "http://kgc.knowledge-graph.jp/data/ResidentPatient/");
			model.setNsPrefix("kdcm", "http://kgc.knowledge-graph.jp/data/CrookedMan/");
			model.setNsPrefix("kddm", "http://kgc.knowledge-graph.jp/data/DancingMen/");
			model.setNsPrefix("kdsb", "http://kgc.knowledge-graph.jp/data/SpeckledBand/");
			
			String rdf_star_type = "";
			if(!mode.equals("0")) {
				rdf_star_type = "_ext"; 
			}
			FileOutputStream fout = new FileOutputStream("rdf-star" + rdf_star_type + ".ttl");
			model.write(fout, "TTL");
			System.out.println("finished");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}