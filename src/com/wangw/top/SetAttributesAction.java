package com.wangw.top;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ww on 2018/1/2.
 */
public class SetAttributesAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        //当前编辑文档
        Document document = e.getData(PlatformDataKeys.EDITOR).getDocument();
        //当前编辑对象
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        //当前工程
        Project project = e.getRequiredData(CommonDataKeys.PROJECT);
        //选中模型
        SelectionModel selectionModel = editor.getSelectionModel();
        //获取光标所在位置(当前编辑区的索引)
        int start = selectionModel.getSelectionStart();
        //根据索引，获取当前光标所在行号(从0开始)
        int lineNum = document.getLineNumber(start);
        //当前行首位置索引
        int lineStart = document.getLineStartOffset(lineNum);
        //计算当前光标左侧的空格数
        int count = start - lineStart;
        if (count < 0) {
            count = 0;
        }

        //获取光标所在处上一行的文本
        String preLineText = getPreLineText(document, editor);
        if (preLineText == null) {
            return;
        }
        //获取实例类型和实例名称
        String[] strings = getClassNameAndInstanceName(preLineText);
        if (strings == null) {
            return;
        }

        //获取实例类型的class对象
        PsiClass psiClass = getPsiClass(document.getText(), strings[0], project);
        if (psiClass == null) {
            return;
        }

        //获取所有set方法名称
        List<String> nameList = getAllMethodNameOfSet(psiClass);
        if (nameList.isEmpty()) {
            return;
        }

        //生成设置属性的代码
        String codeStr = buildCodeStr(nameList, count, strings[1]);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                document.insertString(start, codeStr);
            }
        };
        //执行插入代码命令
        WriteCommandAction.runWriteCommandAction(project, runnable);
    }

    /**
     * 获取指定类的所有set方法
     * @param psiClass
     * @return
     */
    private List<String> getAllMethodNameOfSet(PsiClass psiClass) {
        PsiMethod[] methods = psiClass.getAllMethods();
        List<String> list = new ArrayList<>();
        for (PsiMethod method : methods) {
            if (method.getName().startsWith("set")) {
                list.add(method.getName());
            }
        }
        return list;
    }

    /**
     * 生成设置属性的代码窜
     * @param nameList  方法名称列表
     * @param count     格式化空格数
     * @param instanceName  实例名称
     * @return
     */
    private String buildCodeStr(List<String> nameList, int count, String instanceName) {
        String str = StringUtils.repeat(" ", count);
        StringBuilder sb = new StringBuilder();
        for (String s : nameList) {
            if (count > 0 && sb.length() > 0) {
                sb.append(str);
            }
            sb.append(instanceName + ".");
            sb.append(s);
            sb.append("();");
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 获取当前的包名
     * @param text
     * @return
     */
    private String getPackageName(String text) {
        Pattern pattern = Pattern.compile("package ((\\w|\\.)+)");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    /**
     * 获取类型和实例名称
     * @param lineText  实例所在行内容
     * @return
     */
    private String[] getClassNameAndInstanceName(String lineText) {
        String[] strings = lineText.split("=");
        if (strings.length < 2) {
            return null;
        }
        strings = strings[0].split("\\s+");
        List<String> list = new ArrayList<>();
        for (String s : strings) {
            if (!s.trim().equals("")) {
                list.add(s);
            }
        }
        if (list.size() >= 2) {
            return new String[]{list.get(0), list.get(1)};
        } else {
            return null;
        }
    }

    /**
     * 获取全类名
     * @param text  当前文档的内容
     * @param className 实例类型名称
     * @return
     */
    private String getFullClassName(String text, String className) {
        //如果包含"." 说明为全路径名，直接返回
        if (className.contains(".")) {
            return className;
        }
        Pattern pattern = Pattern.compile("import ((\\w|\\.)+" + className + ");");
        Matcher matcher = pattern.matcher(text);
        String fullClassName = "";
        while (matcher.find()) {
            fullClassName =  matcher.group(1);
            if (StringUtils.isNotBlank(fullClassName)) {
                return fullClassName;
            }
        }
        if ("".equals(fullClassName)) {
            return getPackageName(text) + "." + className;
        } else {
            return null;
        }
    }

    /**
     * 获取所有使用"*" 导入的包名称
     * @param text
     * @return
     */
    private List<String> getAllImportPackageNameOfStar(String text) {
        Pattern pattern = Pattern.compile("import ((\\w|\\.)+\\*)");
        Matcher matcher = pattern.matcher(text);
        List<String> packageList = new ArrayList<>();
        while (matcher.find()) {
            String packageName =  matcher.group(1);
            if (StringUtils.isNotBlank(packageName)) {
                packageList.add(packageName.substring(0, packageName.length() - 2));
            }
        }
        return packageList;
    }

    /**
     * 获取指定类的class
     * @param text
     * @param className
     * @param project
     * @return
     */
    private PsiClass getPsiClass(String text, String className, Project project) {
        //获取全类名(自带包名-->不同包中-->同一包中-->默认包中)
        String fullClassName = getFullClassName(text, className);
        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(fullClassName, GlobalSearchScope.allScope(project));
        if (psiClass != null) {
            return psiClass;
        }

        //如果以上方式获取不到class对象，则按使用"*"导入的方式
        List<String> packageList = getAllImportPackageNameOfStar(text);
        for (String s : packageList) {
            psiClass = JavaPsiFacade.getInstance(project).findClass(s + "." + className, GlobalSearchScope.allScope(project));
            if (psiClass != null) {
                return psiClass;
            }
        }
        return null;
    }

    /**
     * 获取实例所在行的内容
     * @param document
     * @param editor
     * @return
     */
    private String getPreLineText(Document document, Editor editor) {
        int curLineNum = document.getLineNumber(editor.getSelectionModel().getSelectionStart());
        for (int i = curLineNum; i >= 0 ; i--) {
            String text = document.getCharsSequence().subSequence(document.getLineStartOffset(i),
                    document.getLineEndOffset(i)).toString();
            if (!text.trim().equals("")) {
                return text;
            }
        }
        return null;
    }
}
