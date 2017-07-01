package gabrielbot.lang.compiler;

import gabrielbot.lang.common.ConstantPool;
import gabrielbot.lang.common.Constants;
import gabrielbot.lang.common.Opcodes;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    private static final int COMMENT_SINGLE_LINE = 1, COMMENT_BLOCK = 2;
	private static final Pattern
		METHOD_CALL_PATTERN = Pattern.compile(
			"\\S+?\\s*\\(.*?\\)", Pattern.MULTILINE
		),
		IF_PATTERN = Pattern.compile(
			"if\\s*\\((?:\\([^()]*(?:\\([^()]*\\)|.)*?[^()]*\\)|.)*?\\)\\s*\\{(?:\\{[^{}]*(?:\\{[^{}]*}|.)*?[^{}]*}|.)*?}", Pattern.MULTILINE
		),
		IF_CONDITION_PATTERN = Pattern.compile(
			"\\((?:\\([^()]*(?:\\([^()]*\\)|.)*?[^()]*\\)|.)*?\\)", Pattern.MULTILINE
		),
		ASSIGNMENT_PATTERN = Pattern.compile(
			"^((local|global)\\s+)?\\S+\\s*=\\s*((local|global)\\s+)?.+", Pattern.MULTILINE
		),
		OPERATION_PATTERN = Pattern.compile(
			"((-?(\\d|\\.)+)|(global\\s+|local\\s+)?\\S+)(\\s*[-+/*%^]\\s*((-?(\\d|\\.)+)|(global\\s+|local\\s+)?\\S+))+", Pattern.MULTILINE
		),
		STRING_CONCAT_PATTERN = Pattern.compile(
			"(\\S+)+\\s*(\\.\\.\\s*(\\S+)+)+", Pattern.MULTILINE
		);
	
	public static byte[] parse(String s) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ClassWriter cw = new ClassWriter(baos);
		ConstantPool.Builder builder = new ConstantPool.Builder();
		List<MethodWriter> writers = new ArrayList<>();
		
		s = stripComments(s);
		for(String m : methods(s)) {
		    boolean extern = m.startsWith("extern");
		    String name = m.substring(m.indexOf(' ', extern ? m.indexOf(' ')+1 : 0), m.indexOf('(')).trim();
			validateName(name);
			String args = m.substring(m.indexOf(' ', extern ? m.indexOf(' ')+1 : 0) + 2 + name.length(), m.indexOf(')')).trim();
            List<String> locals = args.isEmpty() ? new ArrayList<>() : args(args);
            if(extern) {
                MethodWriter method = cw.method(name, MethodWriter.ACC_STATIC | MethodWriter.EXTERN, locals.size());
                writers.add(method);
                continue;
            }
			String body = body(m);
			body = body.equals("{}") ? body : body.substring(1, body.length()-1);

			MethodWriter method = cw.method(name, locals.size());
			writers.add(method);
			code(builder, method, name, body, locals);
		}
		
		cw.writeConstants(builder.build());
		cw.method("main",0).writeOpcode(Opcodes.VRETURN).writeToClass();
		for(MethodWriter mw : writers) {
			mw.writeOpcode(Opcodes.VRETURN).writeToClass();
		}
		
		return baos.toByteArray();
	}

	private static List<String> methods(String s) {
        StringBuilder sb = new StringBuilder();
        boolean inString = false, escaped = false, extern = false;
        int braces = 0, index = 0;
        List<String> methods = new ArrayList<>();
        for(char c : s.toCharArray()) {
            index++;
            switch(c) {
                case ')':
                    sb.append(c);
                    if(extern) {
                        methods.add(sb.toString().trim());
                        sb = new StringBuilder();
                        extern = false;
                    }
                    break;
                case '{':
                    if(inString) {
                        sb.append(c);
                        break;
                    }
                    braces++;
                    sb.append(c);
                    break;
                case '}':
                    sb.append(c);
                    if(inString) {
                        break;
                    }
                    braces--;
                    if(braces == 0) {
                        methods.add(sb.toString().trim());
                        sb = new StringBuilder();
                    } else if(braces < 0) {
                        throw new IllegalArgumentException("Unexpected } at index " + index + ": " + sb);
                    }
                    break;
                case '"':
                    escaped = false;
                    if(!escaped) {
                        inString = !inString;
                    }
                    sb.append(c);
                    break;
                case '\\':
                    escaped = !escaped;
                    sb.append(c);
                    break;
                case ';':
                    if(braces != 0) {
                        sb.append(c);
                    }
                    break;
                default:
                    if(sb.toString().trim().equals("extern")) {
                        extern = true;
                    }
                    sb.append(c);
                    break;
            }
        }
        if(sb.toString().trim().length() > 0)throw new IllegalArgumentException("Unclosed body: " + sb);
        return methods;
    }

	private static String body(String method) {
	    StringBuilder sb = new StringBuilder();
	    boolean bodyStart = false, inString = false, escaped = false;
	    int braces = 0, index = 0;
	    for(char c : method.toCharArray()) {
	        index++;
	        if(c == ')' && !bodyStart) {
	            bodyStart = true;
	            continue;
            }
	        if(bodyStart) switch(c) {
                case '{':
                    if(inString) {
                        sb.append(c);
                        break;
                    }
                    braces++;
                    sb.append(c);
                    break;
                case '}':
                    sb.append(c);
                    if(inString) {
                        break;
                    }
                    braces--;
                    if(braces == 0) {
                        return sb.toString().trim();
                    } else if(braces < 0) {
                        throw new IllegalArgumentException("Unexpected } at index " + index + ": " + method);
                    }
                    break;
                case '"':
                    escaped = false;
                    if(!escaped) {
                        inString = !inString;
                    }
                    sb.append(c);
                    break;
                case '\\':
                    escaped = !escaped;
                    sb.append(c);
                    break;
                default:
                    sb.append(c);
            }
        }
        throw new IllegalArgumentException("Unclosed body: " + method);
    }
	
	private static void code(ConstantPool.Builder builder, MethodWriter method, String name, String body, List<String> locals) {
		for(String statement : statements(name, body)) {
			Matcher ifMatcher = IF_PATTERN.matcher(statement);
			boolean b = false;
			while(ifMatcher.find()) {
				b = true;
				String block = ifMatcher.group();
				Matcher conditionMatcher = IF_CONDITION_PATTERN.matcher(block);
				if(!conditionMatcher.find()) throw new AssertionError(statement);
				String conditionWithParentheses = conditionMatcher.group();
				String condition = conditionWithParentheses.replaceAll("^\\(+", "").replaceAll("\\)+$", "");
				String ifCode = block.substring(2 /*if*/ + conditionWithParentheses.length() + 1 /*{*/, block.length()-1);
				if(condition.equals("true")) {
					code(builder, method, name, ifCode, locals);
				} else if(condition.matches("(\\d|.)+[lLfFdD]?\\s*==\\s*(\\d|.)+[lLfFdD]?")) {
					String num1 = condition.substring(0, condition.indexOf('=')).trim();
					String num2 = condition.substring(condition.lastIndexOf('=')+1, condition.length()).trim();
					if(!Character.isDigit(num1.charAt(num1.length()-1))) num1 = num1.substring(0, num1.length()-1);
					if(!Character.isDigit(num2.charAt(num2.length()-1))) num2 = num2.substring(0, num2.length()-1);
					boolean equals;
					if(num1.contains(".") || num2.contains(".")) {
						equals = Double.parseDouble(num1) == Double.parseDouble(num2);
					} else {
						equals = Long.parseLong(num1) == Long.parseLong(num2);
					}
					if(equals) {
						code(builder, method, name, ifCode, locals);
					}
				}
			}
			if(b) continue;
			String varname = null;
			boolean globalVar = false;
			String array = null;
			if(statement.matches("^((local|global)\\s+)?\\S+\\s*=.+")) {
				String s = statement.substring(0, statement.indexOf('='));
				String[] parts = s.split("\\s+");
				if(parts.length == 2) {
					if(!(parts[0].equals("global") || parts[0].equals("local"))) {
						throw new IllegalArgumentException("Illegal var modifier: " + parts[0]);
					}
					globalVar = parts[0].equals("global");
					varname = parts[1];
				} else {
					varname = parts[0];
				}
				if(varname.indexOf('[') > 0) {
					array = varname.substring(varname.indexOf('['));
					varname = varname.substring(0, varname.indexOf('['));
				}
				validateName(varname);
			}
			Matcher assignmentMatcher = ASSIGNMENT_PATTERN.matcher(statement);
			if(assignmentMatcher.find()) {
				String a = assignmentMatcher.group();
				a = a.substring(a.indexOf('=')+1).trim();
				Matcher stringConcatMatcher = STRING_CONCAT_PATTERN.matcher(a);
				if(stringConcatMatcher.find()) {
					String s = stringConcatMatcher.group();
					StringBuilder sb = new StringBuilder();
					method.writeOpcode(Opcodes.NEW, builder.add(Constants.NEW_STRING));
					boolean inString = false, escaped = false;
					char lastChar = 0;
					for(char c : s.toCharArray()) {
						switch(c) {
							case '.':
								if(!inString && lastChar == '.') {
									sb.deleteCharAt(sb.length()-1);
									load(builder, method, locals, sb.toString());
									method.writeOpcode(Opcodes.INVOKEVIRTUAL, builder.add(Constants.STRING_APPEND), 1);
									sb = new StringBuilder();
									break;
								}
								sb.append(c);
								break;
							case '"':
								sb.append(c);
								if(!escaped) inString = !inString;
								break;
							case '\\':
								if(escaped) {
									sb.append(c);
								}
								escaped = !escaped;
								break;
							default:
								sb.append(c);
						}
						lastChar = c;
					}
					load(builder, method, locals, sb.toString());
					method.writeOpcode(Opcodes.INVOKEVIRTUAL, builder.add(Constants.STRING_APPEND), 1);
					setVar(builder, method, locals, globalVar, varname, array);
					continue;
				}
				Matcher operationMatcher = OPERATION_PATTERN.matcher(a);
				if(operationMatcher.find()) {
					String op = operationMatcher.group();
					StringBuilder sb = new StringBuilder();
					Opcodes nextOpcode = null;
					if(array != null) setVar(builder, method, locals, globalVar, varname, array);
					for(char c : op.toCharArray()) {
						switch(c) {
							case '+':
								load(builder, method, locals, sb.toString());
								sb = new StringBuilder();
								if(nextOpcode != null) method.writeOpcode(nextOpcode);
								nextOpcode = Opcodes.ADD;
								break;
							case '-':
								if(sb.length() == 0) { //negative numbers
									sb.append(c);
									break;
								}
								load(builder, method, locals, sb.toString());
								sb = new StringBuilder();
								if(nextOpcode != null) method.writeOpcode(nextOpcode);
								nextOpcode = Opcodes.SUB;
								break;
							case '*':
								load(builder, method, locals, sb.toString());
								sb = new StringBuilder();
								if(nextOpcode != null) method.writeOpcode(nextOpcode);
								nextOpcode = Opcodes.MUL;
								break;
							case '/':
								load(builder, method, locals, sb.toString());
								sb = new StringBuilder();
								if(nextOpcode != null) method.writeOpcode(nextOpcode);
								nextOpcode = Opcodes.DIV;
								break;
							case '%':
								load(builder, method, locals, sb.toString());
								sb = new StringBuilder();
								if(nextOpcode != null) method.writeOpcode(nextOpcode);
								nextOpcode = Opcodes.MOD;
								break;
							case '^':
								load(builder, method, locals, sb.toString());
								sb = new StringBuilder();
								if(nextOpcode != null) method.writeOpcode(nextOpcode);
								nextOpcode = Opcodes.POW;
								break;
							case ' ': break;
							default:
								sb.append(c);
								break;
						}
					}
					load(builder, method, locals, sb.toString());
					method.writeOpcode(nextOpcode);
				} else {
					load(builder, method, locals, a);
				}
				setVar(builder, method, locals, globalVar, varname, array);
			}
			Matcher callMatcher = METHOD_CALL_PATTERN.matcher(statement);
			if(callMatcher.find()) {
				String call = callMatcher.group();
				if(callMatcher.find()) throw new IllegalArgumentException("Invalid code: " + statement);
				String methodName = call.substring(0, call.indexOf('(')).trim();
				validateName(methodName);
				
				String methodArgs = call.substring(call.indexOf('(')+1, call.length()-1);
				
				List<String> argss = methodArgs.isEmpty() ? new ArrayList<>() : args(methodArgs);
				for(String a : argss) {
					if(a.isEmpty()) {
						throw new IllegalArgumentException("Empty parameter: " + methodArgs);
					}
					load(builder, method, locals, a);
				}
					
				method.writeOpcode(Opcodes.INVOKESTATIC, builder.add(methodName), argss.size());
				if(varname != null) {
					setVar(builder, method, locals, globalVar, varname, array);
				}
			}
		}
	}
	
	private static void setVar(ConstantPool.Builder builder, MethodWriter method, List<String> locals, boolean globalVar, String varname, String array) {
		if(array == null) {
			if(globalVar) {
				method.writeOpcode(Opcodes.STOREGLOBAL, builder.add(varname));
			} else {
				if(!locals.contains(varname)) {
					locals.add(varname);
				}
				method.writeOpcode(Opcodes.STORE, locals.indexOf(varname));
			}
		} else {
			if(globalVar) {
				method.writeOpcode(Opcodes.LOADGLOBAL, builder.add(varname));
			} else {
				int idx = locals.indexOf(varname);
				if(idx == -1) throw new IllegalArgumentException("Undeclared var: " + varname);
				method.writeOpcode(Opcodes.LOAD, idx);
			}
			StringBuilder sb = new StringBuilder();
			char[] chars = array.toCharArray();
			for(int i = 0; i < chars.length; i++) {
				char c = chars[i];
				switch(c) {
					case '[': break;
					case ']':
						try {
							if(i == chars.length-1) {
								method.writeOpcode(Opcodes.ASTORE, Integer.parseInt(sb.toString()), 1);
							} else {
								method.writeOpcode(Opcodes.ALOAD, Integer.parseInt(sb.toString()));
							}
							sb = new StringBuilder();
						} catch(NumberFormatException e) {
							throw new IllegalArgumentException("Invalid int: " + sb);
						}
						break;
					default:
						sb.append(c);
				}
			}
		}
	}
	
	private static void load(ConstantPool.Builder builder, MethodWriter method, List<String> locals, String a) {
		String t = a.trim();
		if(a.replace(" ", "").equals("{}")) {
			method.writeOpcode(Opcodes.NEW, builder.add(Constants.NEW_ARRAY));
		} else if(t.startsWith("{") && t.endsWith("}")) {
			method.writeOpcode(Opcodes.NEW, builder.add(Constants.NEW_ARRAY));
			List<String> elements = args(a.substring(1, a.length()-1));
			int i = 0;
			for(String s : elements) {
				load(builder, method, locals, s);
				method.writeOpcode(Opcodes.ASTORE, i++, 2);
			}
		} else if(Character.isDigit(a.charAt(0)) || (a.length() > 1 && (a.charAt(0) == '-' || a.charAt(0) == '+') && Character.isDigit(a.charAt(1)))) {
			loadNumber(method, a);
		} else if(a.charAt(0) == '"') {
			String str = a.substring(1, a.lastIndexOf('"'));
			method.writeOpcode(Opcodes.LOADCONSTANT, builder.add(str));
		} else {
			loadVar(builder, method, locals, a);
		}
	}
	
	private static void loadVar(ConstantPool.Builder builder, MethodWriter method, List<String> locals, String var) {
		if(var.matches("^global\\s+\\S+")) {
			var = var.substring(var.indexOf(' ')).trim();
			String p = var.indexOf('[') > 0 ? var.substring(var.indexOf('[')) : null;
			if(p != null) {
				var = var.substring(0, var.indexOf('[')-1).trim();
			}
			method.writeOpcode(Opcodes.LOADGLOBAL, builder.add(var));
			if(p != null) {
				arrayLoad(method, p);
			}
			return;
		} else if(var.matches("^local\\s+\\S+")) {
			String n = var.substring(var.indexOf(' ')).trim();
			String p = n.indexOf('[') > 0 ? n.substring(n.indexOf('[')) : null;
			if(p != null) {
				n = n.substring(0, n.indexOf('[')-1).trim();
			}
			int i = locals.indexOf(n);
			if(i == -1) {
				throw new IllegalArgumentException("Undeclared local var: " + n);
			}
			method.writeOpcode(Opcodes.LOAD, i);
			if(p != null) {
				arrayLoad(method, p);
			}
			return;
		}
		String p = var.indexOf('[') > 0 ? var.substring(var.indexOf('[')) : null;
		if(p != null) {
			var = var.substring(0, var.indexOf('[')).trim();
		}
		int i = locals.indexOf(var);
		if(i == -1) {
			method.writeOpcode(Opcodes.LOADGLOBAL, builder.add(var));
		} else {
			method.writeOpcode(Opcodes.LOAD, i);
		}
		if(p != null) {
			arrayLoad(method, p);
		}
	}
	
	private static void arrayLoad(MethodWriter method, String p) {
		StringBuilder sb = new StringBuilder();
		for(char c : p.toCharArray()) {
			switch(c) {
				case '[': break;
				case ']':
					try {
						method.writeOpcode(Opcodes.ALOAD, Integer.parseInt(sb.toString()));
						sb = new StringBuilder();
					} catch(NumberFormatException e) {
						throw new IllegalArgumentException("Invalid int: " + sb);
					}
					break;
				default:
					sb.append(c);
			}
		}
	}
	
	private static void loadNumber(MethodWriter method, String a) {
		if(a.matches("-?\\d+(L|l)")) {
			String n = a.substring(0, a.length()-1);
			try {
				long l = Long.parseLong(n);
				method.writeOpcode(Opcodes.LCONST, (int)(l>>32), (int)(l));
			} catch(NumberFormatException e) {
				throw new IllegalArgumentException("Invalid long: " + a);
			}
		} else if(a.matches("-?(\\d|.)+(D|d)")) {
			String n = a.substring(0, a.length()-1);
			try {
				double d = Double.parseDouble(n);
				long l = Double.doubleToLongBits(d);
				method.writeOpcode(Opcodes.DCONST, (int)(l>>32), (int)(l));
			} catch(NumberFormatException e) {
				throw new IllegalArgumentException("Invalid double: " + a);
			}
		} else if(a.matches("-?(\\d|.)+(F|f)")) {
			String n = a.substring(0, a.length()-1);
			try {
				float f = Float.parseFloat(n);
				method.writeOpcode(Opcodes.FCONST, Float.floatToIntBits(f));
			} catch(NumberFormatException e) {
				throw new IllegalArgumentException("Invalid float: " + a);
			}
		} else {
			try {
				method.writeOpcode(Opcodes.ICONST, Integer.parseInt(a));
			} catch(NumberFormatException e) {
				throw new IllegalArgumentException("Invalid int: " + a);
			}
		}
	}
	
	private static List<String> statements(String name, String s) {
		List<String> statements = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		boolean inString = false, escaped = false, inParens = false;
		int block = 0;
		for(char c : s.toCharArray()) {
			switch(c) {
				case '{':
					if(!inString && !inParens) {
						block++;
					}
					sb.append(c);
					break;
				case '}':
					if(!inString && !inParens) {
						block--;
						if(block == 0) {
							sb.append(c);
							statements.add(sb.toString().trim());
							sb = new StringBuilder();
							break;
						}
					}
					sb.append(c);
					break;
				case '(':
					if(!inString) {
						inParens = true;
					}
					sb.append(c);
					break;
				case ')':
					if(!inString) {
						inParens = false;
					}
					sb.append(c);
					break;
				case ';':
					if(inString) {
						sb.append(c);
					} else if(block == 0) {
						statements.add(sb.toString().trim());
						sb = new StringBuilder();
					} else {
						sb.append(c);
					}
					break;
				case '"':
					escaped = false;
					if(!escaped) {
						inString = !inString;
					}
					sb.append(c);
					break;
				case '\\':
					escaped = !escaped;
				default:
					sb.append(c);
					break;
			}
		}
		String l = sb.toString().trim();
		if(l.length() > 0) {
			throw new IllegalArgumentException("Leftover code on function " + name + ": " + l);
		}
		return statements;
	}
	
	private static List<String> args(String s) {
		List<String> args = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		boolean inString = false, escaped = false;
		for(char c : s.toCharArray()) {
			switch(c) {
				case ',':
					if(inString) {
						sb.append(c);
					} else {
						args.add(sb.toString().trim());
						sb = new StringBuilder();
					}
					break;
				case '"':
					escaped = false;
					if(!escaped) {
						inString = !inString;
					}
					sb.append(c);
					break;
				case '\\':
					escaped = !escaped;
				default:
					sb.append(c);
					break;
			}
		}
		args.add(sb.toString().trim());
		
		
		return args;
	}
	
	private static void validateName(String name) {
		if(Character.isDigit(name.charAt(0))) {
			throw new IllegalArgumentException("Invalid name: " + name);
		}
		for(char c : name.toCharArray()) {
			if(!(Character.isAlphabetic(c) || Character.isDigit(c) || c == '_' || c == '[' || c == ']')) {
				throw new IllegalArgumentException("Invalid name: " + name);
			}
		}
	}
	
	private static String stripComments(String s) {
		StringBuilder sb = new StringBuilder();
		
		boolean inString = false, escaped = false;
		int inComment = 0;
		char lastChar = 0;
		for(char c : s.toCharArray()) {
			if(inComment == COMMENT_SINGLE_LINE) {
				if(c == '\n') {
					inComment = 0;
				}
				continue;
			}
			switch(c) {
				case '\n': break;
				case '*':
					if(lastChar == '/' && !inString) {
						sb.deleteCharAt(sb.length()-1);
						inComment = COMMENT_BLOCK;
					} else {
						sb.append(c);
					}
					break;
				case '/':
					if(lastChar == '*' && !inString) {
						sb.deleteCharAt(sb.length()-1);
						inComment = 0;
					} else if(lastChar == '/' && !inString) {
						sb.deleteCharAt(sb.length()-1);
						inComment = COMMENT_SINGLE_LINE;
					} else {
						sb.append(c);
					}
					break;
				case '"':
					escaped = false;
					if(!escaped) {
						inString = !inString;
					}
					sb.append(c);
					break;
				case '\\':
					escaped = !escaped;
				default:
					if(inComment == 0) sb.append(c);
					break;
			}
			lastChar = c;
		}
		
		return sb.toString();
	}
}
