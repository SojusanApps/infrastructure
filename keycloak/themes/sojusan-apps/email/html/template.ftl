<#macro emailLayout>
<!DOCTYPE html>
<html lang="${locale.language}" dir="${(ltr)?then('ltr','rtl')}">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <style>
    body, table, td { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }
    a { color: #5b4fd6; }
    p { margin: 0 0 16px; line-height: 1.6; }
    p:last-child { margin-bottom: 0; }
  </style>
</head>
<body style="margin:0; padding:0; background-color:#f5f3fb;">
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f5f3fb;">
    <tr>
      <td align="center" style="padding:40px 16px;">
        <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="max-width:480px;">
          <tr>
            <td align="center" style="padding-bottom:24px;">
              <img src="${url.resourcesUrl}/img/app-logo.png" alt="Sojusan Auth" height="44" style="height:44px; width:auto; display:block; border:0;">
            </td>
          </tr>
          <tr>
            <td style="background-color:#ffffff; border-radius:16px; padding:32px; box-shadow:0 12px 30px rgba(28,24,48,0.08); color:#1c1830; font-size:15px; line-height:1.6;">
              <#nested>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
</#macro>
