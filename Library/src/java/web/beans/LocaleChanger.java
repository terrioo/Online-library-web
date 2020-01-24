package web.beans;

import java.io.Serializable;
import java.util.Locale;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

@ManagedBean(eager = true)
@SessionScoped
public class LocaleChanger implements Serializable {

    private Locale currentLocale;

    public void changeLocale(String localeCode) {
        currentLocale = new Locale(localeCode);
        
    }

    public Locale getCurrentLocale() {
        

        if (currentLocale == null) {
            currentLocale = new Locale("ru");// по-умолчанию - русский язык
        }        
        
        return currentLocale;

    }
    

}
