/*
 * This is free software, licensed under the Gnu Public License (GPL) get a copy from <http://www.gnu.org/licenses/gpl.html> $Id:
 * TableRenderer.java,v 1.7 2005-06-18 04:58:13 hzeller Exp $ author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.view;

import henplus.view.Column;
import henplus.view.ColumnMetaData;


/**
 * document me.
 */
public interface ITableRenderer {

    public void addRow(Column[] row);

    public ColumnMetaData[] getMetaData();

    public void closeTable();

    public void flush();

}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
