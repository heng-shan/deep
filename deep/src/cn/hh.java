package cn.njust.cy;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.rewrite.*;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.*;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.texteditor.ITextEditor;
 
public class hh {
	
	public hh() {
		build();
	}
	
	private static void build() {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource("".toCharArray());
		
		CompilationUnit comp = (CompilationUnit) parser.createAST(null); 
		comp.recordModifications();
		
		AST ast = comp.getAST();
		
		/*
		*public class HelloWorld {
		*
		*}
		*/
		TypeDeclaration classDec = ast.newTypeDeclaration();
		classDec.setInterface(false);//设置为非接口
		
		SimpleName className = ast.newSimpleName("HelloWorld");
		Modifier classModifier = ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
		
		//设置类节点
		classDec.setName(className);//类名
		classDec.modifiers().add(classModifier);//类可见性
		
		//将类节点连接为编译单元的子节点
		comp.types().add(classDec);		
		
		/*
		*public class HelloWorld {
		*  public HelloWorld(){
		*    
		*  }
		*}
		*/
		MethodDeclaration methodDec = ast.newMethodDeclaration();
		methodDec.setConstructor(true);//设置为构造函数
		
		SimpleName methodName = ast.newSimpleName("HelloWorld");
		Modifier methodModifier = ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
		Block methodBody = ast.newBlock();
 
		//设置方法节点
		methodDec.setName(methodName);// 方法名
		methodDec.modifiers().add(methodModifier);//方法可见性
		methodDec.setBody(methodBody); //方法体
		
		//将方法节点连接为类节点的子节点
		classDec.bodyDeclarations().add(methodDec);
		
		/*
		*public class HelloWorld {
		*  public HelloWorld(){
		*    System.out.println("Hello World!");
		*  }
		*}
		*/
		MethodInvocation methodInv = ast.newMethodInvocation();
		
		SimpleName nameSystem = ast.newSimpleName("System");
		SimpleName nameOut = ast.newSimpleName("out");
		SimpleName namePrintln = ast.newSimpleName("println");
		
		//连接‘System’和‘out’
		//System.out
		QualifiedName nameSystemOut = ast.newQualifiedName(nameSystem, nameOut);
		
		//连接‘System.out’和‘println’到MethodInvocation节点
		//System.out.println()
		methodInv.setExpression(nameSystemOut);
		methodInv.setName(namePrintln);
		
		//”Hello World!”
		StringLiteral sHelloworld = ast.newStringLiteral();
		sHelloworld.setEscapedValue("\"Hello World!\"");
		
		//System.out.println(“Hello World!”)
		methodInv.arguments().add(sHelloworld);
		
		//将方法调用节点MethodInvocation连接为表达式语句ExpressionStatement的子节点
		//System.out.println(“Hello World!”);
		ExpressionStatement es = ast.newExpressionStatement(methodInv);
		
		//将表达式语句ExpressionStatement连接为方法节点的点
		methodBody.statements().add(es);
		
		//End
		System.out.println(comp.toString());
	}
	public static void main(String[] args) {
		build();
	}
}

