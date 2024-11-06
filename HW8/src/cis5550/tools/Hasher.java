package cis5550.tools;

import java.security.*;
import java.util.*;

public class Hasher {

  protected static final String[] byte2chars = {
    "aa","ba","ca","da","ea","fa","ga","ha","ia","ja","ka","la","ma","na","oa","pa",
    "qa","ra","sa","ta","ua","va","wa","xa","ya","za","ac","bc","cc","dc","ec","fc",
    "gc","hc","ic","jc","kc","lc","mc","nc","oc","pc","qc","rc","sc","tc","uc","vc",
    "wc","xc","yc","zc","ae","be","ce","de","ee","fe","ge","he","ie","je","ke","le",
    "me","ne","oe","pe","qe","re","se","te","ue","ve","we","xe","ye","ze","ag","bg",
    "cg","dg","eg","fg","gg","hg","ig","jg","kg","lg","mg","ng","og","pg","qg","rg",
    "sg","tg","ug","vg","wg","xg","yg","zg","ai","bi","ci","di","ei","fi","gi","hi",
    "ii","ji","ki","li","mi","ni","oi","pi","qi","ri","si","ti","ui","vi","wi","xi",
    "yi","zi","ak","bk","ck","dk","ek","fk","gk","hk","ik","jk","kk","lk","mk","nk",
    "ok","pk","qk","rk","sk","tk","uk","vk","wk","xk","yk","zk","am","bm","cm","dm",
    "em","fm","gm","hm","im","jm","km","lm","mm","nm","om","pm","qm","rm","sm","tm",
    "um","vm","wm","xm","ym","zm","ao","bo","co","do","eo","fo","go","ho","io","jo",
    "ko","lo","mo","no","oo","po","qo","ro","so","to","uo","vo","wo","xo","yo","zo",
    "aq","bq","cq","dq","eq","fq","gq","hq","iq","jq","kq","lq","mq","nq","oq","pq",
    "qq","rq","sq","tq","uq","vq","wq","xq","yq","zq","as","bs","cs","ds","es","fs",
    "gs","hs","is","js","ks","ls","ms","ns","os","ps","qs","rs","ss","ts","us","vs"
  };

  public static String hash(String x) {
    String sha1 = "";
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      md.reset();
      md.update(x.getBytes("UTF-8"));
      byte[] digest = md.digest();
      for (int i=0; i<digest.length; i++)
        sha1 = sha1 + byte2chars[(digest[i]>0) ? digest[i] : 255+digest[i]];
    } catch (Exception e) { e.printStackTrace(); }
    return sha1;
  }

  public static void main(String args[]) {
    for (int i=0; i<10000; i++)
      System.out.println(hash(""+i));
  }
}