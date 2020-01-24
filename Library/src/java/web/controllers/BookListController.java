package web.controllers;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import web.beans.Book;
import web.db.Database;
import web.enums.SearchType;

@ManagedBean(eager = true)
@SessionScoped
public class BookListController implements Serializable {

    private ArrayList<Book> currentBookList; 
    private ArrayList<Integer> pageNumbers = new ArrayList<Integer>(); 

    private char selectedLetter; 
    private SearchType selectedSearchType = SearchType.TITLE;
    private int selectedGenreId; 
    private String currentSearchString; 
    private String currentSqlNoLimit;

    private boolean pageSelected;
    private int booksCountOnPegh = 2;
    private long selectedPageNumber = 1; 
    private long totalBooksCount; 
    //-------
    private boolean editModeView;

    public BookListController() {
        fillBooksAll();
    }

    private void submitValues(Character selectedLetter, long selectedPageNumber, int selectedGenreId, boolean requestFromPager) {
        this.selectedLetter = selectedLetter;
        this.selectedPageNumber = selectedPageNumber;
        this.selectedGenreId = selectedGenreId;
        this.pageSelected = requestFromPager;

    }

    //<editor-fold defaultstate="collapsed" desc="запросы в базу">
    private void fillBooksBySQL(String sql) {
        
        StringBuilder sqlBuilder = new StringBuilder(sql);
        
        currentSqlNoLimit = sql;
        
        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;
        
        try {
            conn = Database.getConnection();
            stmt = conn.createStatement();
            
            if (!pageSelected) {
                
                rs = stmt.executeQuery(sqlBuilder.toString());
                rs.last();
                
                totalBooksCount = rs.getRow();
                fillPageNumbers(totalBooksCount, booksCountOnPegh);
                
            }
            
            if (totalBooksCount > booksCountOnPegh) {
                sqlBuilder.append(" limit ").append(selectedPageNumber * booksCountOnPegh - booksCountOnPegh).append(",").append(booksCountOnPegh);
            }
            
            rs = stmt.executeQuery(sqlBuilder.toString());
            
            currentBookList = new ArrayList<Book>();
            
            while (rs.next()) {
                Book book = new Book();
                book.setId(rs.getLong("id"));
                book.setName(rs.getString("name"));
                book.setGenre(rs.getString("genre"));
                book.setIsbn(rs.getString("isbn"));
                book.setAuthor(rs.getString("author"));
                book.setPageCount(rs.getInt("page_count"));
                book.setPublishDate(rs.getInt("publish_year"));
                book.setPublisher(rs.getString("publisher"));
                book.setDescr(rs.getString("descr"));
                currentBookList.add(book);
            }
            
        } catch (SQLException ex) {
            Logger.getLogger(BookListController.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(BookListController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }
    
    private void fillBooksAll() {
        fillBooksBySQL("select b.id,b.name,b.isbn,b.page_count,b.publish_year, p.name as publisher, b.descr, "
                + "a.fio as author, g.name as genre, b.image from library.book b inner join author a on b.author_id=a.id "
                + "inner join genre g on b.genre_id=g.id inner join publisher p on b.publisher_id=p.id order by b.name");
    }
    
    public String fillBooksByGenre() {
        
        imitateLoading();
        
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        
        selectedGenreId = Integer.valueOf(params.get("genre_id"));
        
        submitValues(' ', 1, selectedGenreId, false);
        
        fillBooksBySQL("select b.id,b.name,b.isbn,b.page_count,b.publish_year, p.name as publisher, a.fio as author, g.name as genre, b.descr, b.image from library.book b "
                + "inner join author a on b.author_id=a.id "
                + "inner join genre g on b.genre_id=g.id "
                + "inner join publisher p on b.publisher_id=p.id "
                + "where genre_id=" + selectedGenreId + " order by b.name ");
        
        
        
        return "books";
    }
    
    public String fillBooksByLetter() {
        
        imitateLoading();
        
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        selectedLetter = params.get("letter").charAt(0);
        
        submitValues(selectedLetter, 1, -1, false);
        
        
        fillBooksBySQL("select b.id,b.name,b.isbn,b.page_count,b.publish_year, p.name as publisher, a.fio as author, g.name as genre, b.descr, b.image from library.book b "
                + "inner join author a on b.author_id=a.id "
                + "inner join genre g on b.genre_id=g.id "
                + "inner join publisher p on b.publisher_id=p.id "
                + "where substr(b.name,1,1)='" + selectedLetter + "' order by b.name ");
        
        return "books";
    }
    
    public String fillBooksBySearch() {
        
        imitateLoading();
        
        submitValues(' ', 1, -1, false);
        
        if (currentSearchString.trim().length() == 0) {
            fillBooksAll();
            return "books";
        }
        
        StringBuilder sql = new StringBuilder("select b.descr, b.id,b.name,b.isbn,b.page_count,b.publish_year, p.name as publisher, a.fio as author, g.name as genre, b.image from library.book b "
                + "inner join author a on b.author_id=a.id "
                + "inner join genre g on b.genre_id=g.id "
                + "inner join publisher p on b.publisher_id=p.id ");
        
        if (selectedSearchType == SearchType.AUTHOR) {
            sql.append("where lower(a.fio) like '%" + currentSearchString.toLowerCase() + "%' order by b.name ");
            
        } else if (selectedSearchType == SearchType.TITLE) {
            sql.append("where lower(b.name) like '%" + currentSearchString.toLowerCase() + "%' order by b.name ");
        }
        
        
        
        fillBooksBySQL(sql.toString());
        
        
        return "books";
    }
    
    public byte[] getContent(int id) {
        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;
        
        
        byte[] content = null;
        try {
            conn = Database.getConnection();
            stmt = conn.createStatement();
            
            rs = stmt.executeQuery("select content from library.book where id=" + id);
            while (rs.next()) {
                content = rs.getBytes("content");
            }
        } catch (SQLException ex) {
            Logger.getLogger(Book.class
                    .getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(Book.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return content;
        
    }
    
    public byte[] getImage(int id) {
        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;
        
        byte[] image = null;
        
        try {
            conn = Database.getConnection();
            stmt = conn.createStatement();
            
            rs = stmt.executeQuery("select image from library.book where id=" + id);
            while (rs.next()) {
                image = rs.getBytes("image");
            }
            
        } catch (SQLException ex) {
            Logger.getLogger(Book.class
                    .getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(Book.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return image;
    }
    
    public String updateBooks() {
        imitateLoading();
        
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        Connection conn = null;
        
        try {
            conn = Database.getConnection();
            prepStmt = conn.prepareStatement("update book set name=?, isbn=?, page_count=?, publish_year=?, descr=? where id=?");
            
            
            for (Book book : currentBookList) {
                if (!book.isEdit()) {
                    continue;
                }
                prepStmt.setString(1, book.getName());
                prepStmt.setString(2, book.getIsbn());
                prepStmt.setInt(3, book.getPageCount());
                prepStmt.setInt(4, book.getPublishDate());
                prepStmt.setString(5, book.getDescr());
                prepStmt.setLong(6, book.getId());
                prepStmt.addBatch();
            }
            
            
            prepStmt.executeBatch();
            
            
        } catch (SQLException ex) {
            Logger.getLogger(BookListController.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (prepStmt != null) {
                    prepStmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(BookListController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        cancelEditMode();
        
        return "books";
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="режим редактирования">
    public void showEdit() {
        editModeView = true;
    }
    
    public void cancelEditMode() {
        editModeView = false;
        for (Book book : currentBookList) {
            book.setEdit(false);
        }
    }
    //</editor-fold>
    
    public Character[] getRussianLetters() {
        Character[] letters = new Character[]{'А', 'Б', 'В', 'Г', 'Д', 'Е', 'Ё', 'Ж', 'З', 'И', 'Й', 'К', 'Л', 'М', 'Н', 'О', 'П', 'Р', 'С', 'Т', 'У', 'Ф', 'Х', 'Ц', 'Ч', 'Ш', 'Щ', 'Ъ', 'Ы', 'Ь', 'Э', 'Ю', 'Я',};
        return letters;
    }

    public void searchStringChanged(ValueChangeEvent e) {
        currentSearchString = e.getNewValue().toString();
    }

    public void searchTypeChanged(ValueChangeEvent e) {
        selectedSearchType = (SearchType) e.getNewValue();
    }

    //<editor-fold defaultstate="collapsed" desc="постраничность">
    public void changeBooksCountOnPage(ValueChangeEvent e) {
        imitateLoading();
        cancelEditMode();
        pageSelected = false;
        booksCountOnPegh = Integer.valueOf(e.getNewValue().toString()).intValue();
        selectedPageNumber = 1;
        fillBooksBySQL(currentSqlNoLimit);
    }

    public void selectPage() {
        cancelEditMode();
        imitateLoading();
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        selectedPageNumber = Integer.valueOf(params.get("page_number"));
        pageSelected = true;
        fillBooksBySQL(currentSqlNoLimit);

    }

    private void fillPageNumbers(long totalBooksCount, int booksCountOnPage) {
        
        pageNumbers.clear();

        
        if (totalBooksCount <= 0 ){
            return;
        }
        
        int pageCount = (int)totalBooksCount/booksCountOnPage;
        
        int ord = (int)totalBooksCount % booksCountOnPage;
        
        if (ord>0){
            pageCount += 1 ;
        }
        

        for (int i = 1; i <= pageCount; i++) {
            pageNumbers.add(i);
        }

    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="гетеры сетеры">
    public boolean isEditMode() {
        return editModeView;
    }

    public ArrayList<Integer> getPageNumbers() {
        return pageNumbers;
    }

    public void setPageNumbers(ArrayList<Integer> pageNumbers) {
        this.pageNumbers = pageNumbers;
    }

    public String getSearchString() {
        return currentSearchString;
    }

    public void setSearchString(String searchString) {
        this.currentSearchString = searchString;
    }

    public SearchType getSearchType() {
        return selectedSearchType;
    }

    public void setSearchType(SearchType searchType) {
        this.selectedSearchType = searchType;
    }

    public ArrayList<Book> getCurrentBookList() {
        return currentBookList;
    }

    public void setTotalBooksCount(long booksCount) {
        this.totalBooksCount = booksCount;
    }

    public long getTotalBooksCount() {
        return totalBooksCount;
    }

    public int getSelectedGenreId() {
        return selectedGenreId;
    }

    public void setSelectedGenreId(int selectedGenreId) {
        this.selectedGenreId = selectedGenreId;
    }

    public char getSelectedLetter() {
        return selectedLetter;
    }

    public void setSelectedLetter(char selectedLetter) {
        this.selectedLetter = selectedLetter;
    }

    public int getBooksOnPage() {
        return booksCountOnPegh;
    }

    public void setBooksOnPage(int booksOnPage) {
        this.booksCountOnPegh = booksOnPage;
    }

    public void setSelectedPageNumber(long selectedPageNumber) {
        this.selectedPageNumber = selectedPageNumber;
    }

    public long getSelectedPageNumber() {
        return selectedPageNumber;
    }
    //</editor-fold>

    private void imitateLoading() {
        try {
            Thread.sleep(500);// имитация загрузки процесса
        } catch (InterruptedException ex) {
            Logger.getLogger(BookListController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
