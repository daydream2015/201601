package aquaint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import aquaint.Aquaint.AnnotDocu;
import aquaint.Aquaint.AnnotMention;
import aquaint.Aquaint.Candi;
import it.acubelab.tagme.Anchor;
import it.acubelab.tagme.AnnotatedText;
import it.acubelab.tagme.Annotation;
import it.acubelab.tagme.Disambiguator;
import it.acubelab.tagme.RelatednessMeasure;
import it.acubelab.tagme.RhoMeasure;
import it.acubelab.tagme.Segmentation;
import it.acubelab.tagme.TagmeParser;
import it.acubelab.tagme.config.TagmeConfig;
import it.acubelab.tagme.preprocessing.TopicSearcher;
import it.acubelab.tagme.preprocessing.anchors.LuceneAnchorSearcher;

public class Aquaint {

	private static final String xmlPath = "./aquaint/xml/";
	private static final String rawPath = "./aquaint/rawText/";
	private static final String ANNOT_FILE="./aquaint/annot/0125-afterlink.txt";	//0124-all-id-c.txt
	private static final String READ_ANNOT_FILE="./aquaint/annot/0125-id-c-i-m.txt";
	private int correctMentionNum=0;	//num of mention which is the same in xml and tagme result
	private int correctEntityNum =0;	//num of mention which both mention and entity is correct in xml and tagme result
	private int mentionNum =0;		//num of mention in xml file
	private int mentionNumAnnotated =0;
	private List<AnnotDocu> sourceDocList=new ArrayList<AnnotDocu>();
	private List<AnnotDocu> annotDocIdList=new ArrayList<AnnotDocu>();
	private List<AnnotDocu> readAnnotDocList=new ArrayList<AnnotDocu>();
	
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		Aquaint p =new Aquaint();
		/*List<String> fileList=p.getPathFiles(xmlPath);
		//p.readXmlGetMention("APW19980603_0791.htm", 0);
		
		for(int i=0;i<fileList.size();i++) {
			p.readXmlGetMention(fileList.get(i),i);
		}*/

		//p.generateCandidate();
		//p.writeAnnotFile();
		//p.countUnambiguousMention();
		//p.writeCandidateListFile();
		p.readAnnotationFile();
		p.processScore();
		//p.annotDocIdList=p.readAnnotDocList;
		p.writeAnnotFile(p.readAnnotDocList);
		//System.out.println(p.sourceDocList.size());
	}

	
	public List<String> getPathFiles(String path) {
		List<String> fileList=new ArrayList<String>();
		File[] files = new File(path).listFiles();
		for (File file : files) {
		    fileList.add(file.getName());
		}
		//System.out.println(fileList);
		return fileList;
	}
	
	/**
	 * read xml file ,analyze it
	 * store mention and entity in xmlMap
	 * use this as golden standard
	 */
	public void readXmlGetMention(String fileName,int did) {
		try {
			AnnotDocu annDoc = new AnnotDocu();
			annDoc.setDocId(did);
			annDoc.setDocName(fileName);
			File xmlFile = new File(xmlPath+fileName);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(xmlFile);
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("ReferenceInstance");
			AnnotMention am = null;
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				//System.out.println(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element element = (Element) nNode;
					StringTokenizer token = new StringTokenizer(element.getElementsByTagName("SurfaceForm").item(0).getTextContent().trim(),"/");
					String title ="";
					while(token.hasMoreTokens())
						title = token.nextToken();	//last token: Brain_tumor
					//System.out.println(title);
					am=new AnnotMention(title);
					annDoc.getMentionList().add(am);
					//xmlMap.put(element.getElementsByTagName("SurfaceForm").item(0).getTextContent().trim().toLowerCase(), title.toLowerCase());
				}	//toLowerCase
			}
			//System.out.println(xmlMap);
			//mentionNum = xmlMap.size();
			sourceDocList.add(annDoc);
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }
	}
	
	/** not use
	 * use tagme system Anchor Dictionary to annotate raw text file 
	 * store annotated mention and entity in tagMap
	 * @throws Exception
	 */
	public void getCandidate(String fileName) throws Exception {
		
		TagmeConfig.init("./config.xml");
        String rawFile = rawPath+fileName;
        String input = "";
        String line;
        try {
            FileReader fileReader = new FileReader(rawFile);
            BufferedReader bufferedReader =new BufferedReader(fileReader);
            while((line = bufferedReader.readLine()) != null) {
                input += line;
            }   
            bufferedReader.close();         
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
		String lang = "en";
		//System.out.println("input text:"+input);
		AnnotatedText ann_text = new AnnotatedText(input);

		RelatednessMeasure rel = RelatednessMeasure.create(lang);

		TagmeParser parser = new TagmeParser(lang, true);
		Disambiguator disamb = new Disambiguator(lang);
		Segmentation segmentation = new Segmentation();
		RhoMeasure rho = new RhoMeasure();

		parser.parse(ann_text);
		segmentation.segment(ann_text);
		disamb.disambiguate(ann_text, rel);
		rho.calc(ann_text, rel);

		List<Annotation> annots = ann_text.getAnnotations();
		TopicSearcher searcher = new TopicSearcher(lang);
		//mentionNumAnnotated = annots.size();
		for (Annotation a : annots) {
			if (a.isDisambiguated() && a.getRho() >= 0.1) {
				String wikipage = searcher.getTitle(a.getTopic());
				System.out.println(wikipage);
				wikipage  = wikipage.replaceAll(" ", "_").toLowerCase();	//toLowerCase
				//tagMap.put(ann_text.getOriginalText(a).toLowerCase(),wikipage );	//toLowerCase
			}
		}
		//mentionNumAnnotated = tagMap.size();
	}

	/** not use
	 * 0123 over 
	 * input readXmlGetMention  result truth document list
	 * output ANNOT_FILE
	 */
	public void writeAnnotationFile() throws IOException {
		
		TagmeConfig.init("./config.xml");
		String lang = "en";
		LuceneAnchorSearcher anchorSearcher = new LuceneAnchorSearcher(lang);
		TopicSearcher topicSearcher = new TopicSearcher(lang);
		
		PrintWriter pw  = new PrintWriter(new FileWriter(ANNOT_FILE));
		for(int k=0;k<annotDocIdList.size();k++) {
			AnnotDocu doc=annotDocIdList.get(k);
			pw.print("-DOCSTART-");
			pw.println(k);
			for(int j=0;j<doc.getMentionList().size();j++) {
				
				AnnotMention mention=doc.getMentionList().get(j);
				//truth-mention as a whole phrase
				String m=mention.getMention();
				m=m.toLowerCase();//.replaceAll("[^A-Za-z0-9' ]", " ");	//to process ping-pong, but failed to keep latin alphabet
				//m=m.toLowerCase();	//keep latin(but stil  no candidate), fail ping-pong
				Anchor a = anchorSearcher.search(m);	//search truth-mention in Anchor Dictionary
				//System.out.println(m.toLowerCase());
				if(a!=null) {
					pw.print(doc.getMentionList().get(j).getMention()+":");
					for(int i=0;i<a.ambiguity();i++) {	//a.ambiguity();	--q.length
						pw.print(a.pageByIndex(i)+"\t");	//wiki wid
						//pw.print(topicSearcher.getTitle(a.pageByIndex(i))+"\t");	//page title of every candidate
					}
					pw.println();
				}else {
					System.out.println(m+"-< NIL>");	//"No candidates for mention - "+
				
					//truth-mention as separate word
					StringTokenizer st = new StringTokenizer(m);
					String candidateOfWord="";
					int count =st.countTokens();
					//System.out.println(count);
					String[] sa=new String[count];
					int i=0;
					while(st.hasMoreTokens())
						sa[i++]=st.nextToken();
					//System.out.println("after while"+sa);
					if((count>1)) {
						for(int start=0;start<count+1;start++) {
							for (int end=start+1;end<count+1;end++) {
								String sb="";
								for(int index=start;index<end;index++){
									if(!sb.equals(""))  
										sb+=" ";
									sb+= sa[index];
								}
								//System.out.println(sb);
								Anchor an = anchorSearcher.search(sb);	//search truth-mention in Anchor Dictionary
								//System.out.println(str.toLowerCase());
								if(an!=null) {
									//pw.print(str+":");
									for(int p=0;p<an.ambiguity();p++) {	//a.ambiguity();	--q.length
										//candidateOfWord+=an.pageByIndex(p)+"\t";	//wiki wid
										pw.print(an.pageByIndex(p)+"\t");
										//pw.print(topicSearcher.getTitle(an.pageByIndex(i))+"\t");	//wiki page title of every candidate
									}
									//pw.println();
								}else {
									System.out.println(sb+"-<NIL>");	//"No candidates for  word - "+
								}
								
							}
						}
					}
					
					/*if(!candidateOfWord.equals("")) {
						pw.println(candidateOfWord);
					}*/
				}
			}
			//end one doc
		}	//end for document
		pw.println("-FILEEND-");
		pw.close();
		
		System.out.println("**********************");
	}
	
	/**
	 * 0124 over 
	 * use truth document list &	tagme Anchor Dictionary  
	 * result: annotDocIdList	(candidate list)
	 */
	public void generateCandidate() throws IOException {
		
		TagmeConfig.init("./config.xml");
		String lang = "en";
		LuceneAnchorSearcher anchorSearcher = new LuceneAnchorSearcher(lang);
		TopicSearcher topicSearcher = new TopicSearcher(lang);
		//PrintWriter pw  = new PrintWriter(new FileWriter(ANNOT_FILE));
		for(int k=0;k<sourceDocList.size();k++) {
			AnnotDocu doc=sourceDocList.get(k);
			AnnotDocu annotDoc = new AnnotDocu();
			annotDoc.docName=doc.docName;
			annotDoc.docId=k;
			//pw.print("-DOCSTART-");
			//pw.println(k);
			for(int j=0;j<doc.getMentionList().size();j++) {
				AnnotMention mention=doc.getMentionList().get(j);
				//truth-mention as a whole phrase
				String m=mention.getMention();
				AnnotMention annotM = new AnnotMention(m);
				annotDoc.getMentionList().add(annotM);
				m=m.toLowerCase();//.replaceAll("[^A-Za-z0-9' ]", " ");	//to process ping-pong, but failed to keep latin alphabet
				//m=m.toLowerCase();	
				Anchor a = anchorSearcher.search(m);	//search truth-mention in Anchor Dictionary
				//System.out.println(m.toLowerCase());
				if(a!=null) {
					//pw.print(doc.getMentionList().get(j).getMention()+":");
					for(int i=0;i<a.ambiguity();i++) {	//a.ambiguity();	--q.length
						//pw.print(a.pageByIndex(i)+"\t");	//wiki wid
						//pw.print(topicSearcher.getTitle(a.pageByIndex(i))+"\t");	//page title of every candidate
						Candi cand=null;
						if(a.ambiguity()>1) {
							 cand=new Candi(a.pageByIndex(i));
							cand.commonness = a.commonness(a.pageByIndex(i));
						}
						else {
							cand =new Candi(a.singlePage());
							cand.commonness = 1.0;
						}
						//can.canTitle=topicSearcher.getTitle(a.pageByIndex(i));
						annotM.candidateList.add(cand);
					}
					//pw.println();
				}else {
					//System.out.println(m+"-< NIL>");	//"No candidates for mention - "+
				
					//truth-mention as separate word
					StringTokenizer st = new StringTokenizer(m);
					String candidateOfWord="";
					int count =st.countTokens();
					//System.out.println(count);
					String[] sa=new String[count];
					int i=0;
					while(st.hasMoreTokens())
						sa[i++]=st.nextToken();
					//System.out.println("after while"+sa);
					if((count>1)) {
						for(int start=0;start<count+1;start++) {
							for (int end=start+1;end<count+1;end++) {
								String sb="";
								for(int index=start;index<end;index++){
									if(!sb.equals(""))  
										sb+=" ";
									sb+= sa[index];
								}
								//sb: sub-phrase of whole mention
								//System.out.println(sb);
								Anchor an = anchorSearcher.search(sb);	//search in Anchor Dictionary
								if(an!=null) {
									for(int p=0;p<an.ambiguity();p++) {	//a.ambiguity();	--q.length
										//candidateOfWord+=an.pageByIndex(p)+"\t";	//wiki wid
										//pw.print(an.pageByIndex(p)+"\t");
										//pw.print(topicSearcher.getTitle(an.pageByIndex(i))+"\t");	//wiki page title of every candidate
										Candi can=null;
										if(an.ambiguity()>1) {
											 can=new Candi(an.pageByIndex(p));
											can.commonness = an.commonness(an.pageByIndex(p));
										}
										else {
											can =new Candi(an.singlePage());
											can.commonness = 1.0;
										}
										//can.canTitle=topicSearcher.getTitle(an.pageByIndex(i));
										annotM.candidateList.add(can);
									}
									//pw.println();
								}else {
									//System.out.println(sb+"-<NIL>");	//"No candidates for  word - "+
								}
							}
						}
					}
				}
			}
			//end one doc
			annotDocIdList.add(annotDoc);
		}	//end for documents
		//pw.println("-FILEEND-");
		//pw.close();
		
		//System.out.println("**********************");
	}
	
	/**
	 * 0124 over 
	 * use truth document list &	tagme Anchor Dictionary  
	 * output annotDocIdList	(candidate list)  & format file
	 */
	public void writeCandidateListFile() throws IOException {
		
		TagmeConfig.init("./config.xml");
		String lang = "en";
		LuceneAnchorSearcher anchorSearcher = new LuceneAnchorSearcher(lang);
		TopicSearcher topicSearcher = new TopicSearcher(lang);
		PrintWriter pw  = new PrintWriter(new FileWriter(ANNOT_FILE));
		for(int k=0;k<sourceDocList.size();k++) {
			AnnotDocu doc=sourceDocList.get(k);
			AnnotDocu annotDoc = new AnnotDocu();
			annotDoc.docName=doc.docName;
			annotDoc.docId=k;
			pw.print("-DOCSTART-"+"\t");
			pw.print(k+"\t");
			pw.println(doc.docName);
			for(int j=0;j<doc.getMentionList().size();j++) {
				AnnotMention mention=doc.getMentionList().get(j);
				//truth-mention as a whole phrase
				String m=mention.getMention();
				AnnotMention annotM = new AnnotMention(m);
				annotDoc.getMentionList().add(annotM);
				m=m.toLowerCase();//.replaceAll("[^A-Za-z0-9' ]", " ");	//to process ping-pong, but failed to keep latin alphabet
				Anchor a = anchorSearcher.search(m);	//search truth-mention in Anchor Dictionary
				//System.out.println(m);
				pw.print(doc.getMentionList().get(j).getMention()+":");
				if(a!=null) {
					
					for(int i=0;i<a.ambiguity();i++) {	//a.ambiguity();	--q.length
						Candi cand=null;
						if(a.ambiguity()>1) {
							 cand=new Candi(a.pageByIndex(i));
							cand.commonness = a.commonness(a.pageByIndex(i));
						}
						else {
							cand =new Candi(a.singlePage());
							cand.commonness = 1.0;
						}
						//cand.canTitle=topicSearcher.getTitle(cand.candId);
						annotM.candidateList.add(cand);
						pw.print(cand.candId);	//wiki wid
						pw.print(";"+cand.commonness);	//commonness
						//pw.print(cand.canTitle+";");	//page title of every candidate
						pw.print("\t");
					}
					//pw.println();
				}else {
					//System.out.println(m+" < NIL >");	//"No candidates for mention - "+
				
					//truth-mention as separate word
					StringTokenizer st = new StringTokenizer(m);
					String candidateOfWord="";
					int count =st.countTokens();
					//System.out.println(count);
					String[] sa=new String[count];
					int i=0;
					while(st.hasMoreTokens())
						sa[i++]=st.nextToken();
					//System.out.println("after while"+sa);
					if((count>1)) {
						for(int start=0;start<count+1;start++) {
							for (int end=start+1;end<count+1;end++) {
								String sb="";
								for(int index=start;index<end;index++){
									if(!sb.equals(""))  
										sb+=" ";
									sb+= sa[index];
								}
								//sb: sub-phrase of whole mention
								//System.out.println(sb);
								Anchor an = anchorSearcher.search(sb);	//search in Anchor Dictionary
								if(an!=null) {
									for(int p=0;p<an.ambiguity();p++) {	//a.ambiguity();	--q.length
										Candi can=null;
										if(an.ambiguity()>1) {
											 can=new Candi(an.pageByIndex(p));
											can.commonness = an.commonness(an.pageByIndex(p));
										}
										else {
											can =new Candi(an.singlePage());
											can.commonness = 1.0;
										}
										//cand.canTitle=topicSearcher.getTitle(cand.candId);
										annotM.candidateList.add(can);
										pw.print(can.candId);	//wiki wid
										pw.print(";"+can.commonness);	//commonness
										//pw.print(can.canTitle+";");	//page title of every candidate
										pw.print("\t");
									}
									//pw.println();
								}else {
									//System.out.println(sb+"-<NIL>");	//"No candidates for  word - "+
								}
							}
						}
					}
				}
				pw.println();
			}
			//end one doc
			annotDocIdList.add(annotDoc);
		}	//end for documents
		pw.println("-FILEEND-");
		pw.close();
		
		//System.out.println("**********************");
	}
	
	
	/**
	 * 0124 over 
	 * use annotDocIdList  
	 * output  format file
	 */
	public void writeAnnotFile(List<AnnotDocu> docList) throws IOException {
		
		PrintWriter pw  = new PrintWriter(new FileWriter(ANNOT_FILE));
		for(int k=0;k<docList.size();k++) {
			AnnotDocu doc=docList.get(k);
			pw.print("-DOCSTART-"+"\t");
			pw.print(k+"\t");
			pw.println(doc.docName);
			for(int j=0;j<doc.getMentionList().size();j++) {
				AnnotMention mention=doc.getMentionList().get(j);
				pw.print(mention.getMention()+":");
				for(int t=0;t<mention.candidateList.size();t++) {
					Candi can= mention.candidateList.get(t);
					pw.print(can.candId);	//wiki wid
					pw.print(";"+can.commonness);	//commonness
					pw.print(";"+can.inlinkScore);	
					pw.print(";"+can.mutlinkScore);
					pw.print("\t");
				}
				pw.println();
			}
			//end one doc
		}	//end for documents
		pw.println("-FILEEND-");
		pw.close();
		
		System.out.println("**********************");
	}
	
	/**
	 * not often use
	 * use to output un-ambiguous mention file
	 * docid=6,7,17...   no un-ambiguous mention
	 * other ok
	 * @throws IOException
	 */
	public void countUnambiguousMention() throws IOException  {
		BufferedReader br= new BufferedReader(new FileReader(ANNOT_FILE));
		PrintWriter pw =new PrintWriter(new FileWriter("./aquaint/annot/un-ambiguous-mention.txt"));
		String line="";
		List<AnnotDocu> docList=new ArrayList<AnnotDocu>();
		AnnotDocu doc=null;
		int docid=0;
		while((line=br.readLine())!=null) {
			if(line.startsWith("-DOCSTART")) {	//start of doc
				if(doc==null) {	//first doc
					doc = new AnnotDocu();
					doc.setDocId(docid++);
				}
				else {
					docList.add(doc);
					doc = new AnnotDocu();
					doc.setDocId(docid++);
				}
			}else if(line.startsWith("-FILEEND-")){	//last doc
				docList.add(doc);
			}
			else {
				StringTokenizer st = new StringTokenizer(line,":");
				if(st.countTokens()>1) {	
					String m=st.nextToken();
					AnnotMention aMention=new AnnotMention(m);
					doc.getMentionList().add(aMention);
					String right = st.nextToken();
					StringTokenizer can = new StringTokenizer(right,"\t");
					if(can.countTokens()==1)
						pw.println((docid-1)+"\t"+right);
				}
			}
		}
		pw.close();
		br.close();
	}
	
	/**
	 * 0124
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	public void readAnnotationFile() throws NumberFormatException, IOException {
		BufferedReader br= new BufferedReader(new FileReader(READ_ANNOT_FILE));
		String line="";
		//List<AnnotDocu> docList=new ArrayList<AnnotDocu>();
		AnnotDocu doc=null;
		while((line=br.readLine())!=null) {
			if(line.startsWith("-DOCSTART-")) {	//start of doc
				StringTokenizer docStr = new StringTokenizer(line,"\t");
				docStr.nextToken();
				int docid =Integer.parseInt(docStr.nextToken());
				String docname=docStr.nextToken();
				if(doc==null) {	//first doc
					doc = new AnnotDocu();
				}
				else {
					readAnnotDocList.add(doc);
					doc = new AnnotDocu();
				}
				doc.setDocId(docid);
				doc.docName=docname;
			}else if(line.startsWith("-FILEEND-")){	//last doc
				readAnnotDocList.add(doc);
			}else {	//mention list line
				//Swiss:26748;0.900251030921936 \t 7351032;0.06231583654880524
				StringTokenizer st = new StringTokenizer(line,":");
				int countCandi=st.countTokens();
				if(countCandi>=1) {	
					String m=st.nextToken();	//mention name
					AnnotMention aMention=new AnnotMention(m);
					doc.getMentionList().add(aMention);
					if(countCandi>1) {	//has candidate
						String right = st.nextToken();
						StringTokenizer cans = new StringTokenizer(right,"\t");
						int count = cans.countTokens();
						//one candidate  26748;0.900251030921936 
						while(cans.hasMoreTokens()) {
						//format  id;commonness_s1;inlink;mutuallink;category;word2vec \t  
							String oneCandidate=cans.nextToken();
							StringTokenizer scores=new StringTokenizer(oneCandidate,";");
							String pid=scores.nextToken();
							String commonness = scores.nextToken();
							String inlink=scores.nextToken();
							String mutlink = scores.nextToken();
							/*String category=scores.nextToken();
							String w2v = scores.nextToken();*/
							
							/*//format  id \t id
							String pid=cans.nextToken();*/
							Candi cand = new Candi(Integer.parseInt(pid));
							aMention.getCandidateList().add(cand);
							cand.commonness=Double.parseDouble(commonness);
							if(count==1) {
								doc.commonSet.add(cand.candId);
								cand.inlinkScore=1.0;
								cand.mutlinkScore=1.0;
								cand.categoryScore=1.0;
								cand.w2vScore=1.0;
								continue;
							}
							cand.inlinkScore=Double.parseDouble(inlink);
							cand.mutlinkScore=Double.parseDouble(mutlink);
							/*cand.categoryScore=Double.parseDouble(category);
							cand.w2vScore=Double.parseDouble(w2v);*/
							
							
						}
					}
				}
			}
		}
		br.close();
	}
	
	public void processScore()  {
		
		for(int k=0;k<readAnnotDocList.size();k++) {	//one doc
			AnnotDocu doc=readAnnotDocList.get(k);
			for(int j=0;j<doc.getMentionList().size();j++) {	//one mention
				AnnotMention mention=doc.getMentionList().get(j);
				List<Integer> deleteIndex = new ArrayList<Integer>();
				for(int t=0;t<mention.candidateList.size();t++) {	//one candidate
					Candi can= mention.candidateList.get(t);
					if((can.inlinkScore==0.0)&&(can.mutlinkScore==0.0))
						deleteIndex.add(t);
				}
				
				for(int index=deleteIndex.size()-1;index>=0;index--) {
					mention.candidateList.remove((int)deleteIndex.get(index));
				}
			}
			//end one doc
		}	//end for documents
		
		System.out.println("**********************");
	}
	
	
	
	/*public void computeInlinkScore() throws WikiApiException {
		Jwpl jwpl = new Jwpl();
		for(int k=0;k<readAnnotDocList.size();k++) {	//one doc
			AnnotDocu doc=readAnnotDocList.get(k);
			for(int j=0;j<doc.getMentionList().size();j++) {	//one mention
				AnnotMention mention=doc.getMentionList().get(j);
				int size=mention.candidateList.size();
				for(int t=0;t<size;t++) {	//one candidate
					if(size>1) {
						double inlinkScore = 0.0;
						int mutlinkScore = 0;
						Candi can= mention.candidateList.get(t);
						for(Integer common:doc.commonSet) {
							inlinkScore+= jwpl.computeInlinkScore(can.candId, common);
							mutlinkScore +=jwpl.computeMutualLinkScore(can.candId, common);
						}
						can.inlinkScore = inlinkScore;
						can.mutlinkScore = mutlinkScore;
					}
					
				}//candidate
			}//mention
			System.out.println(k);
			//end one doc
		}	//end for documents
	}*/
	
	/*public void computeCategoryScore()  {
		for(int k=0;k<readAnnotDocList.size();k++) {	//one doc
			AnnotDocu doc=readAnnotDocList.get(k);
			for(int j=0;j<doc.getMentionList().size();j++) {	//one mention
				AnnotMention mention=doc.getMentionList().get(j);
				int size=mention.candidateList.size();
				for(int t=0;t<size;t++) {	//one candidate
					if(size>1) {
						double categoryScore = 0;
						Candi can= mention.candidateList.get(t);
						WikiCat cat1 = WikiCat.instance(can.candId);
						for(Integer common:doc.commonSet) {
							WikiCat cat2 = WikiCat.instance(common);
							categoryScore+= WikiCat.jaccardSim(cat1.getCatSet(), cat2.getCatSet());
						}
						can.categoryScore = categoryScore;
					}
					
				}//candidate
			}//mention
			System.out.println(k);
			//end one doc
		}	//end for documents
	}*/
	
	class AnnotDocu {
		private List<AnnotMention> mentionList=new ArrayList<AnnotMention>();
		private Set<Integer> commonSet = new HashSet<Integer>();
		private int docId;
		private String docName;
		public int getDocId() {
			return docId;
		}
		public void setDocId(int docId) {
			this.docId = docId;
		}
		public List<AnnotMention> getMentionList() {
			return mentionList;
		}
		public void setMentionList(List<AnnotMention> mentionList) {
			this.mentionList = mentionList;
		}
		public String getDocName() {
			return docName;
		}
		public void setDocName(String docName) {
			this.docName = docName;
		}
		public Set<Integer> getCommonSet() {
			return commonSet;
		}
		public void setCommonSet(Set<Integer> commonSet) {
			this.commonSet = commonSet;
		}
	}
	
	class AnnotMention {
		private List<Candi> candidateList=new ArrayList<Candi>();
		private String mention;
		public AnnotMention(String mention) {
			super();
			this.mention = mention;
		}
		public String getMention() {
			return mention;
		}
		public void setMention(String mention) {
			this.mention = mention;
		}
		public List<Candi> getCandidateList() {
			return candidateList;
		}
		public void setCandidateList(List<Candi> candidateList) {
			this.candidateList = candidateList;
		}
	}
	
	class Candi {
		private int candId;
		private String canTitle;
		private double categoryScore=0.0;
		private double commonness=0.0;
		private double inlinkScore=0.0;
		private double mutlinkScore=0.0;
		private double w2vScore=0.0;
		public Candi(int candId) {
			super();
			this.candId = candId;
		}
		public int getCandId() {
			return candId;
		}
		public void setCandId(int candId) {
			this.candId = candId;
		}
		public String getCanTitle() {
			return canTitle;
		}
		public void setCanTitle(String canTitle) {
			this.canTitle = canTitle;
		}
		public double getCategoryScore() {
			return categoryScore;
		}
		public void setCategoryScore(double categoryScore) {
			this.categoryScore = categoryScore;
		}
		public double getCommonness() {
			return commonness;
		}
		public void setCommonness(double commonness) {
			this.commonness = commonness;
		}
		public double getInlinkScore() {
			return inlinkScore;
		}
		public void setInlinkScore(double inlinkScore) {
			this.inlinkScore = inlinkScore;
		}
		public double getMutlinkScore() {
			return mutlinkScore;
		}
		public void setMutlinkScore(double mutlinkScore) {
			this.mutlinkScore = mutlinkScore;
		}
		public double getW2vScore() {
			return w2vScore;
		}
		public void setW2vScore(double w2vScore) {
			this.w2vScore = w2vScore;
		}
	}
	
	
}
