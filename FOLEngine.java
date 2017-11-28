import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

class Sentence
{
	ArrayList<Term> listOfTerms;
	Sentence()
	{
		this.listOfTerms = new ArrayList<Term>();
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		for (int index = 0; index < listOfTerms.size() ; index++)
		{
			sb.append(listOfTerms.get(index).toString());
			sb.append("|");
		}
		return sb.toString().substring(0, sb.length() - 1);
	}
}

class Term
{
	ArrayList<String> args;
	boolean isNegated;
	String predicate;
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		if (isNegated)
		{
			sb.append("~");
		}
		else
		{
			sb.append("");
		}
		sb.append(predicate);
		sb.append("(");
		sb.append(args);
		sb.append(")");
		
		return sb.toString();
	}
}

public class FOLEngine
{
	ArrayList<Sentence> kb = new ArrayList<Sentence>();
	int kbSize = -1;
	HashMap<Integer, ArrayList<String>> predicateMap;
	HashMap<String, HashSet<Integer>> mapOfMatchingSent;
	long timer ;
	
	public static void main(String[] args) throws IOException
	{
		FOLEngine rfol = new FOLEngine();
		BufferedReader br = new BufferedReader(new FileReader("input.txt"));
		String line = br.readLine();
		int numberOfQueries = Integer.parseInt(line);
		int count = numberOfQueries;
		ArrayList<String> queries = new ArrayList<String>();
		while (count > 0)
		{
			queries.add(br.readLine());
			count--;
		}
		
		count = Integer.parseInt(br.readLine());
		Sentence sentence = null;
		while (count > 0)
		{
			sentence = new Sentence();
			String sent = br.readLine();
			Term term = null;
			for (String s : sent.split("\\|"))
			{
				s = s.trim();
				term = new Term();
				ArrayList<String> arguments = new ArrayList<String>();
				String argsArray [] = s.substring(s.indexOf("(")+1,s.indexOf(")")).trim().split(",");
				for (int argIndex = 0; argIndex < argsArray.length; argIndex++)
				{
					arguments.add(argsArray[argIndex].trim());
				}
				term.args = arguments;
				if (s.charAt(0) == '~')
				{
					term.isNegated = true;
					term.predicate = s.substring(1, s.indexOf("(")).trim();
				}
				else
				{
					term.isNegated = false;
					term.predicate = s.substring(0, s.indexOf("(")).trim();
				}
				sentence.listOfTerms.add(term);
			}
			rfol.kb.add(sentence);
			count--;
		}
		
		br.close();
		rfol.kbSize = rfol.kb.size();
		rfol.standardize();
		
		ArrayList<String> resultList = new ArrayList<String>();
		for (int index = 0; index < queries.size(); index++)
		{
			resultList.add("FALSE");
		}
		
		for (int index = 0; index < queries.size(); index++)
		{
			ArrayList<Sentence> loopList = new ArrayList<Sentence>();
			Sentence query = new Sentence();
			Term term = new Term();
			String s = queries.get(index);
			if (s.charAt(0) == '~')
			{
				term.isNegated = false;
				term.predicate = s.substring(1, s.indexOf("(")).trim();
			}
			else
			{
				term.isNegated = true;
				term.predicate = s.substring(0, s.indexOf("(")).trim();
			}
			ArrayList<String> arguments = new ArrayList<String>();
			String argsArray [] = s.substring(s.indexOf("(")+1,s.indexOf(")")).trim().split(",");
			for (int argIndex = 0; argIndex < argsArray.length; argIndex++)
			{
				arguments.add(argsArray[argIndex].trim());
			}
			term.args = arguments;
			ArrayList<Term> al = new ArrayList<Term>();
			al.add(term);
			query.listOfTerms = al;
			rfol.kb.add(query);
			rfol.findPredicates();
			rfol.mapMatchingSentences();
			
			//loopList.add(rfol.kb.get(rfol.kbSize));
			
			rfol.timer = System.currentTimeMillis();
			boolean b = rfol.resolve_query(rfol.kb.get(rfol.kbSize), loopList);
			if (b)
			{
				resultList.set(index, "TRUE");
			}
			
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("output.txt")));
			for (String res : resultList)
			{
				bw.write(res);
				bw.newLine();
			}
			bw.close();
			//System.out.println(b);
			rfol.kb.remove(rfol.kbSize);
		}
	}
	
	public void writeOuput(ArrayList<String> res) throws IOException
	{
		BufferedWriter bw1 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("output.txt")));
		for (String resString : res)
		{
			bw1.write(resString);
			bw1.newLine();
		}
		bw1.close();
	}
	
	public void printKb()
	{
		for (Sentence ss : kb)
		{
			System.out.println(ss.toString());
		}
	}
	public void standardize()
	{
		for (int index  = 0; index < kb.size(); index++)
		{
			ArrayList<Term> myTerms = kb.get(index).listOfTerms;
			for (Term t : myTerms)
			{
				for (int index2 = 0; index2 < t.args.size() ; index2++)
				{
					if (isVar(t.args.get(index2)))
					{
						t.args.set(index2, t.args.get(index2) + index);
					}
				}
			}
		}
	}
	
	public static boolean isVar(String arg)
	{
		if (arg.isEmpty())
			return false;
		return Character.isLowerCase(arg.charAt(0));
	}
	
	public boolean detectLoop_predicates (ArrayList<Term> terms1, ArrayList<Term> terms2)
	{
		int count = 0;
		for (Term term1 : terms1)
		{
			for (Term term2: terms2)
			{
				if (term1.predicate.equals(term2.predicate))
				{
					if (term1.isNegated == term2.isNegated)
					{
						count++;
					}
				}
			}
		}
		
		if (count == terms1.size())
		{
			return true;
		}
		
		return false;
	}
	
	public boolean detectLoop_args(ArrayList<Term> terms1, ArrayList<Term> terms2)
	{
		for (Term term1 : terms1)
		{
			for (Term term2: terms2)
			{
				if (term1.predicate.equals(term2.predicate))
				{
					if (term1.isNegated == term2.isNegated)
					{
						ArrayList<String> args1 = term1.args;
						ArrayList<String> args2 = term2.args;
						for (int index = 0; index < args1.size() ; index++)
						{
							if (!isVar(args1.get(index)) && !isVar(args2.get(index)))
							{
								if (!args1.get(index).equals(args2.get(index)))
								{
									return false;
								}
							}
							else
							if ((isVar(args1.get(index)) && !isVar(args2.get(index))) ||
									(!isVar(args1.get(index)) && isVar(args2.get(index))))
							{
								return false;
							}
						}
					}
				}
			}
		}
		return true;
	}
	
	public boolean detect_2(ArrayList<Term> terms1, ArrayList<Term> terms2)
	{
		HashMap<String, Integer> detectMap = new HashMap<String, Integer>();
		for (int index = 0; index < terms1.size(); index++)
		{
			if (detectMap.containsKey(terms1.get(index).toString()))
			{
				int i = detectMap.get(terms1.get(index).toString());
				detectMap.put(terms1.get(index).toString(), (i + 1));
			}
			else
			{
				detectMap.put(terms1.get(index).toString(), 1);
			}
		}
		
		for (int index = 0; index < terms2.size(); index++)
		{
			if (detectMap.containsKey(terms2.get(index).toString()))
			{
				int i = detectMap.get(terms2.get(index).toString());
				detectMap.put(terms2.get(index).toString(), i - 1);
			}
		}
		
		for (String predicate : detectMap.keySet())
		{
			if (detectMap.get(predicate) != 0)
			{
				return false;
			}
		}
		
		return true;
	}
	
	public boolean detectLoop (Sentence result_1, ArrayList<Sentence> loopList)
	{
		for (int index = 0; index < loopList.size(); index++)
		{
			Sentence result = deepCopy(result_1);
			Sentence loopedSent = deepCopy(loopList.get(index));
			result = deStandardize(result);
			loopedSent = deStandardize(loopedSent);
			if (result.listOfTerms.size() == loopedSent.listOfTerms.size())
			{
				//result = doPre_subsume(result, loopList.get(index));
				if (detect_2(result.listOfTerms, loopedSent.listOfTerms))
				{
					return true;
				}
			}
		}
		return false;
	}
	
	public Sentence factorize (Sentence sentence_1)
	{
		Sentence sentence = deepCopy(sentence_1);
		for (int index = 0; index < sentence.listOfTerms.size(); index++)
		{
			Term term1 = sentence.listOfTerms.get(index);
			for (int index1  = index + 1; index1 < sentence.listOfTerms.size(); index1++)
			{
				Term term2 = sentence.listOfTerms.get(index1);
				if (term1.predicate.equals(term2.predicate) && term1.isNegated == term2.isNegated)
				{
					int c = 0;
					for (int i = 0; i < term1.args.size(); i++)
					{
						if (term1.args.get(i).equals(term2.args.get(i)))
						{
							c++;
						}
					}
					if (c == term1.args.size())
					{
						sentence.listOfTerms.remove(index1);
					}
				}
			}
		}
			
		for (int index = 0; index < sentence.listOfTerms.size(); index++)
		{
			Term term1 = sentence.listOfTerms.get(index);
			for (int index1  = index + 1; index1 < sentence.listOfTerms.size(); index1++)
			{
				Term term2 = sentence.listOfTerms.get(index1);
				if (term1.predicate.equals(term2.predicate) && term1.isNegated == term2.isNegated)
				{
					int c = 0;
					for (int i = 0; i < term1.args.size(); i++)
					{
						if (isVar(term1.args.get(i)) && isVar(term2.args.get(i)))
						{
							c++;
						}
					}
					if (c == term1.args.size())
					{
						sentence.listOfTerms.remove(index);
					}
				}
			}
		}
		
		return sentence;
	}
	
	public Sentence deStandardize(Sentence s)
	{
		HashMap<String, String> hm = new HashMap<String, String>();
		Sentence sentence = deepCopy(s);
		for (int index = 0; index < sentence.listOfTerms.size(); index++)
		{
			Term term = sentence.listOfTerms.get(index);
			for (int argIndex = 0; argIndex < term.args.size(); argIndex++)
			{
				if (isVar(term.args.get(argIndex)))
				{
					if (!hm.containsKey(term.args.get(argIndex)))
					{
						int endIndex = term.args.get(argIndex).indexOf("-");
						endIndex = endIndex == -1 ? term.args.get(argIndex).length() : endIndex;
						hm.put(term.args.get(argIndex), term.args.get(argIndex).substring(0, endIndex));
					}
				}
			}
		}
		
		for (int index = 0; index < sentence.listOfTerms.size(); index++)
		{
			Term term = sentence.listOfTerms.get(index);
			for (int argIndex = 0; argIndex < term.args.size(); argIndex++)
			{
				if (isVar(term.args.get(argIndex)))
				{
					if (hm.containsKey(term.args.get(argIndex)))
					{
						term.args.set(argIndex, hm.get(term.args.get(argIndex)));
					}
				}
			}
		}
		
		return sentence;
	}
	
	public Sentence standardize(Sentence s)
	{
		HashMap<String, String> hm = new HashMap<String, String>();
		Sentence sentence = deepCopy(s);
		for (int index = 0; index < sentence.listOfTerms.size(); index++)
		{
			Term term = sentence.listOfTerms.get(index);
			for (int argIndex = 0; argIndex < term.args.size(); argIndex++)
			{
				if (isVar(term.args.get(argIndex)))
				{
					if (!hm.containsKey(term.args.get(argIndex)))
					{
						hm.put(term.args.get(argIndex), term.args.get(argIndex)+"-" +new Random().nextInt(9));
					}
				}
			}
		}
		
		for (int index = 0; index < sentence.listOfTerms.size(); index++)
		{
			Term term = sentence.listOfTerms.get(index);
			for (int argIndex = 0; argIndex < term.args.size(); argIndex++)
			{
				if (isVar(term.args.get(argIndex)))
				{
					if (hm.containsKey(term.args.get(argIndex)))
					{
						term.args.set(argIndex, hm.get(term.args.get(argIndex)));
					}
				}
			}
		}
		
		return sentence;
	}
	
	int count = 0;
	public boolean resolve_query(Sentence query, ArrayList<Sentence> loopList)
	{
		if (System.currentTimeMillis() > (timer + 90000))
		{
			return false;
		}
		
		ArrayList<Integer> matchingSentences = findMatchingSentences(query);
		for (int index = 0; index < matchingSentences.size(); index++)
		{
			for (Term term : query.listOfTerms)
			{
				for (Term kbTerm : kb.get(matchingSentences.get(index)).listOfTerms)
				{
					if (term.predicate.equals(kbTerm.predicate) && term.isNegated != kbTerm.isNegated)
					{
						HashMap<String, String> substitutions = new HashMap<String, String>();
						unify(term, kbTerm, substitutions);
						if (substitutions.isEmpty())
						{
							continue;
						}
						else
						{
							Sentence result = resolving(query, kb.get(matchingSentences.get(index)), substitutions);
							//System.out.println(result);
							
							if (result.listOfTerms.size() == 0)
							{
								return true;
							}
							
							result = factorize(result);
							boolean b = detectLoop(result, loopList);
							if (result.listOfTerms.size() !=0  && b)
							{
								continue;
							}
							
							if (subsume(result, loopList))
							{
								continue;
							}
							
							if (!loopList.contains(query))
							{
								loopList.add(query);
							}
							
							/*System.out.println("*********************************");
							System.out.println("Query sentence: " + query);
							System.out.println("KB Sentence: " + matchingSentences.get(index));
							System.out.println("Result sentence: " + result);
							
							for (Sentence s : loopList)
							{
								System.out.println(s.toString());
							}*/
							
							result = standardize(result);
							if (resolve_query(result, loopList))
							{
								return true;
							}
							if (System.currentTimeMillis() > (timer + 90000))
							{
								return false;
							}
						}
					}
				}
			}
			
		}
		
		return false;
	}
	
	
	public Sentence doPre_subsume (Sentence result_1, Sentence loopedSent_1)
	{
		Sentence result = deepCopy(result_1);
		Sentence loopedSent = deepCopy(loopedSent_1);
		HashMap<String, String> hm = new HashMap<String, String>();
		for (int index = 0; index < result.listOfTerms.size(); index++)
		{
			for (int index2 = 0; index2 < loopedSent.listOfTerms.size(); index2++)
			{
				if (result.listOfTerms.get(index).predicate.equals(loopedSent.listOfTerms.get(index2).predicate) &&
						result.listOfTerms.get(index).isNegated == loopedSent.listOfTerms.get(index2).isNegated)
				{
					hm = new HashMap<String, String>();
					ArrayList<String> args1 = result.listOfTerms.get(index).args;
					ArrayList<String> args2 = loopedSent.listOfTerms.get(index2).args;
					for (int i = 0; i < args1.size(); i++)
					{
						if (!args1.get(i).equals(args2.get(i)))
						{
							if (isVar(args1.get(i)) && isVar(args2.get(i)))
							{
								hm.put(args1.get(i), args2.get(i));
							}
							else
							{
								hm.clear();
								break;
							}
						}
					}
					for (int t = 0; t < result.listOfTerms.size(); t++)
					{
						for (int a = 0 ; a < result.listOfTerms.get(t).args.size(); a++)
						{
							if (hm.get(result.listOfTerms.get(t).args.get(a)) != null)
							{
								String s = new String(hm.get(result.listOfTerms.get(t).args.get(a)));
								result.listOfTerms.get(t).args.remove(a);
								result.listOfTerms.get(t).args.add(a, s);
							}
						}
					}
				}
			}
		}
		
		return result;
	}
	
	public boolean subsume(Sentence result, ArrayList<Sentence> loopList)
	{
		HashMap<String, Integer> detectMap;
		for (Sentence sentence : loopList)
		{
			result = doPre_subsume(result, sentence);
			detectMap = new HashMap<String, Integer>();
			ArrayList<Term> terms1 = sentence.listOfTerms;
			for (int index = 0; index < terms1.size(); index++)
			{
				if (detectMap.containsKey(terms1.get(index).toString()))
				{
					int i = detectMap.get(terms1.get(index).toString());
					detectMap.put(terms1.get(index).toString(), (i + 1));
				}
				else
				{
					detectMap.put(terms1.get(index).toString(), 1);
				}
			}
			
			ArrayList<Term> terms2 = result.listOfTerms;
			for (int index = 0; index < terms2.size(); index++)
			{
				if (detectMap.containsKey(terms2.get(index).toString()))
				{
					int i = detectMap.get(terms2.get(index).toString());
					detectMap.put(terms2.get(index).toString(), i - 1);
				}
			}
			
			int c = 0;
			for (String predicate : detectMap.keySet())
			{
				if (detectMap.get(predicate) == 0)
				{
					c++;
				}
			}
			
			if (c == detectMap.size())
			{
				return true;
			}
		}
		
		return false;
	}
	
	public Sentence deepCopy (Sentence s)
	{
		Sentence sentence1 = new Sentence();
		ArrayList<Term> resTerms = new ArrayList<Term>(); 
		for (Term term : s.listOfTerms)
		{
			Term resultTerm = new Term();
			resultTerm.predicate = term.predicate;
			resultTerm.isNegated = term.isNegated;
			ArrayList<String> resArgs = new ArrayList<String>();
			for (String arg :term.args)
			{
				String s1 = new String(arg);
				resArgs.add(s1);
			}
			resultTerm.args = resArgs;
			resTerms.add(resultTerm);
		}
		sentence1.listOfTerms = resTerms;
		return sentence1;
	}
	public Sentence resolving(Sentence sentence_1, Sentence sentence_2, HashMap<String, String> substitutions)
	{
		Sentence sentence1 = deepCopy(sentence_1);
		Sentence sentence2 = deepCopy(sentence_2);
		ArrayList<Term> resTerms = new ArrayList<Term>();
		int c = 0;
		for (int index = 0; index < sentence1.listOfTerms.size(); index++)
		{
			for (int index1 = 0; index1 < sentence2.listOfTerms.size(); index1++)
			{
				if (resolve(sentence1.listOfTerms.get(index), sentence2.listOfTerms.get(index1), substitutions))
				{
					c = 1;
					sentence2.listOfTerms.remove(index1);
					sentence1.listOfTerms.remove(index);
					index--;
					break;
				}
			}
			if (c == 1)
			{
				break;
			}
		}
		
		for (Term term : sentence1.listOfTerms)
		{
			Term resultTerm = new Term();
			resultTerm.predicate = term.predicate;
			resultTerm.isNegated = term.isNegated;
			ArrayList<String> resArgs = new ArrayList<String>();
			for (String arg :term.args)
			{
				if (substitutions.get(arg) == null)
				{
					String s1 = new String(arg);
					resArgs.add(s1);
				}
				else
				{
					String s1 = new String(substitutions.get(arg));
					resArgs.add(s1);
				}
			}
			resultTerm.args = resArgs;
			resTerms.add(resultTerm);
		}
		
		for (Term term : sentence2.listOfTerms)
		{
			Term resultTerm = new Term();
			resultTerm.predicate = term.predicate;
			resultTerm.isNegated = term.isNegated;
			ArrayList<String> resArgs = new ArrayList<String>();
			for (String arg :term.args)
			{
				if (substitutions.get(arg) == null)
				{
					String s1 = new String(arg);
					resArgs.add(s1);
				}
				else
				{
					String s1 = new String(substitutions.get(arg));
					resArgs.add(s1);
				}
			}
			resultTerm.args = resArgs;
			resTerms.add(resultTerm);
		}
		
		Sentence resSentence = new Sentence();
		resSentence.listOfTerms = resTerms;
		
		return resSentence;
	}

	public boolean resolve(Term term1, Term term2, HashMap<String, String> substitutions)
	{
		if (term1.predicate.equals(term2.predicate) && term1.isNegated != term2.isNegated)
		{
			int c = 0;
			for (int index = 0; index < term1.args.size(); index++)
			{
				String a = substitutions.get(term1.args.get(index)) != null ? 
							substitutions.get(term1.args.get(index)) :
							term1.args.get(index);
				String b = substitutions.get(term2.args.get(index)) != null ? 
							substitutions.get(term2.args.get(index)) :
								term2.args.get(index);
				
				if (a.equals(b))
				{
					c++;
				}
			}
			if (c == term1.args.size())
			{
				return true;
			}
		}
		
		return false;
	}
	
	
	public void unify(Term term1, Term term2, HashMap<String, String> substitutions)
	{
		ArrayList<String> args1 = term1.args;
		ArrayList<String> args2 = term2.args;
		for (int index = 0; index < args1.size(); index++)
		{
			if (!args1.get(index).equals(args2.get(index)))
			{
				if ((isVar(args1.get(index)) && isVar(args2.get(index))) ||
						(isVar(args1.get(index)) && !isVar(args2.get(index))))
				{
					if (substitutions.get(args1.get(index)) == null)
					{
						substitutions.put(args1.get(index), args2.get(index));
					}
					else
					{
						substitutions.clear();
						break;
					}
				}
				else
				if (!isVar(args1.get(index)) && isVar(args2.get(index)))
				{
					if (substitutions.get(args2.get(index)) == null)
					{
						substitutions.put(args2.get(index), args1.get(index));
					}
					else
					{
						substitutions.clear();
						break;
					}
				}
				else
				if (!isVar(args1.get(index)) && !isVar(args2.get(index)))
				{
					substitutions.clear();
					break;
				}
			}
			else
			{
				if ((isVar(args1.get(index)) && isVar(args2.get(index))) || 
						(!isVar(args1.get(index)) && !isVar(args2.get(index))))
				{
					if (substitutions.get(args1.get(index)) == null)
					{
						substitutions.put(args1.get(index), args2.get(index));
					}
					else
					{
						substitutions.clear();
						break;
					}
				}
			}
		}
	}
	
	public ArrayList<String> find_predicates_no_args(Sentence query)
	{
		ArrayList<String> predicates = new ArrayList<String>();
		for (Term term : query.listOfTerms)
		{
			predicates.add(term.predicate);
		}
		
		return predicates;
	}
	
	public ArrayList<Integer> findMatchingSentences(Sentence query)
	{
		ArrayList<Integer> listOfSentences = new ArrayList<Integer>();
		for (Term term : query.listOfTerms)
		{
			if (term.isNegated)
			{
				// This null check is to avoid NPE when mapOfMatching gives empty
				if (mapOfMatchingSent.get(term.predicate) != null)
				{
					listOfSentences.addAll(mapOfMatchingSent.get(term.predicate));
				}
			}
			else
			{
				// This null check is to avoid NPE when mapOfMatching gives empty
				if (mapOfMatchingSent.get('~' + term.predicate) != null)
				{
					listOfSentences.addAll(mapOfMatchingSent.get('~' + term.predicate));
				}
			}
		}
		
		return listOfSentences;
	}
	
	public void findPredicates()
	{
		predicateMap = new HashMap<Integer, ArrayList<String>>();
		for (int index = 0; index < kb.size(); index++)
		{
			ArrayList<String> predicates = new ArrayList<String>();
			for (Term term : kb.get(index).listOfTerms)
			{
				if (term.isNegated)
				{
					predicates.add('~' + term.predicate);
				}
				else
				{
					predicates.add(term.predicate);
				}
			}
			
			predicateMap.put(index, predicates);
		}
	}
	
	public void mapMatchingSentences()
	{
		mapOfMatchingSent = new HashMap<String, HashSet<Integer>>() ;
		for (int index = 0; index < kb.size(); index++)
		{
			ArrayList<String> predicates = predicateMap.get(index);
			for (String predicate : predicates)
			{
				HashSet<Integer> list;
				if (mapOfMatchingSent.containsKey(predicate))
				{
					list = mapOfMatchingSent.get(predicate);
				}
				else
				{
					list = new HashSet<Integer>();
				}
				
				list.add(index);
				for (int index1 = index + 1; index1 < kb.size(); index1++)
				{
					if (predicateMap.get(index1).contains(predicate))
					{
						list.add(index);
					}
				}
				
				mapOfMatchingSent.put(predicate, list);
			}
		}
	}
}
