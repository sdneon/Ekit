/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hexidec.ekit.component;

import java.awt.Color;
import java.awt.event.ActionEvent;
import javax.swing.SwingWorker;
import javax.swing.text.StyledEditorKit;

/**
 * Delayed StyledEditorKit.ForegroundAction
 * to avoid hanging PieMenu.
 *
 * @author Neon
 */
public class DelayedSekForegroundAction extends StyledEditorKit.ForegroundAction {

    public DelayedSekForegroundAction(String nm, Color fg) {
        super(nm, fg);
    }

    public void _actionPerformed(ActionEvent e)
    {
        super.actionPerformed(e);
    }

    public void actionPerformed(ActionEvent e) {
        final DelayedSekForegroundAction self = this;
        SwingWorker delayedTask = new SwingWorker<Boolean, Boolean>() {
            @Override
            public Boolean doInBackground() {
                return true;
            }

            @Override protected void done() {
                try
                {
                    self._actionPerformed(e);
                } catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }
        };
        delayedTask.execute();
     }
}
